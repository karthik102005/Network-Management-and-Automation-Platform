package com.nmap.network;

import com.nmap.dto.DeviceReachabilityResponseDto;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for testing network reachability to managed network elements.
 * Provides on-demand, bounded reachability checks utilizing ICMP echo with
 * concurrent TCP management-port fallback to avoid false negatives when ICMP is filtered.
 */
@Service
public class DeviceReachabilityService {

    private static final Logger log = LoggerFactory.getLogger(DeviceReachabilityService.class);

    public static final int DEFAULT_TIMEOUT_MS = 2000;
    public static final int MIN_TIMEOUT_MS = 500;
    public static final int MAX_TIMEOUT_MS = 3000;

    // Standard management ports for fallback probes (SSH, HTTP, HTTPS, Telnet)
    private static final int[] MANAGEMENT_PORTS = {22, 80, 443, 23};

    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "reachability-probe");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void cleanup() {
        probeExecutor.shutdownNow();
    }

    /**
     * Checks if a device IP is reachable via ICMP echo (or fallback TCP probe).
     *
     * @param ipAddress Target IP address
     * @param timeoutMs Timeout in milliseconds
     * @return true if reachable within timeout, false otherwise
     */
    public boolean isReachable(String ipAddress, int timeoutMs) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(ipAddress.trim());
            return address.isReachable(timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS);
        } catch (IOException e) {
            log.debug("Reachability check failed for IP [{}]: {}", ipAddress, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a specific network port is open.
     *
     * @param ipAddress Target IP address
     * @param port      Target port number
     * @param timeoutMs Timeout in milliseconds
     * @return true if port connection was established, false otherwise
     */
    public boolean isPortOpen(String ipAddress, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port), timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            log.trace("Port {} closed or unreachable on [{}]: {}", port, ipAddress, e.getMessage());
            return false;
        }
    }

    /**
     * Performs a comprehensive, bounded reachability check on a target device.
     * 1. Validates the management IP format.
     * 2. Attempts an ICMP echo probe within a bounded timeout slice.
     * 3. If ICMP is blocked or unresponsive, launches concurrent TCP management port probes.
     * 4. Distinguishes established TCP connections, TCP resets (active host), timeouts, and inconclusive errors.
     *
     * @param deviceId           Device identifier
     * @param hostname           Device hostname
     * @param ipAddress          Device management IP address
     * @param requestedTimeoutMs Desired timeout budget in milliseconds (bounded between 500ms and 3000ms)
     * @return Detailed diagnostic reachability result DTO
     */
    public DeviceReachabilityResponseDto checkReachability(Long deviceId, String hostname, String ipAddress, Integer requestedTimeoutMs) {
        Instant checkTime = Instant.now();

        // 1. Validate Target IP
        if (ipAddress == null || ipAddress.isBlank()) {
            return new DeviceReachabilityResponseDto(
                    deviceId, hostname, ipAddress, false, "INVALID_TARGET", "NONE", null, null, checkTime,
                    "Target management IP address is missing or blank."
            );
        }

        String targetIp = ipAddress.trim();
        if (!isValidTargetAddress(targetIp)) {
            return new DeviceReachabilityResponseDto(
                    deviceId, hostname, targetIp, false, "INVALID_TARGET", "NONE", null, null, checkTime,
                    "Target IP address format is invalid: '" + targetIp + "'."
            );
        }

        // 2. Bound the timeout budget
        int budgetMs = (requestedTimeoutMs == null || requestedTimeoutMs <= 0)
                ? DEFAULT_TIMEOUT_MS
                : Math.min(Math.max(requestedTimeoutMs, MIN_TIMEOUT_MS), MAX_TIMEOUT_MS);

        long startTime = System.currentTimeMillis();

        // 3. Attempt ICMP Echo Probe (allocated up to 40% of budget or 800ms)
        int icmpTimeout = Math.min(budgetMs / 2, 800);
        long icmpStart = System.currentTimeMillis();
        try {
            InetAddress address = InetAddress.getByName(targetIp);
            if (address.isReachable(icmpTimeout)) {
                long latency = Math.max(1, System.currentTimeMillis() - icmpStart);
                return new DeviceReachabilityResponseDto(
                        deviceId, hostname, targetIp, true, "REACHABLE", "ICMP_ECHO", null, latency, Instant.now(),
                        "Target device responded to ICMP echo probe in " + latency + "ms."
                );
            }
        } catch (UnknownHostException e) {
            return new DeviceReachabilityResponseDto(
                    deviceId, hostname, targetIp, false, "INCONCLUSIVE", "NONE", null, null, Instant.now(),
                    "Unable to resolve host: " + e.getMessage()
            );
        } catch (IOException e) {
            log.debug("ICMP probe failed or blocked for {}: {}", targetIp, e.getMessage());
        }

        // 4. Calculate remaining budget for fallback TCP probes
        long elapsed = System.currentTimeMillis() - startTime;
        long remainingBudget = budgetMs - elapsed;
        if (remainingBudget < 200) {
            return new DeviceReachabilityResponseDto(
                    deviceId, hostname, targetIp, false, "UNREACHABLE", "NONE", null, null, Instant.now(),
                    "Probe budget exhausted during ICMP probe (" + elapsed + "ms elapsed)."
            );
        }

        int socketTimeout = (int) Math.min(remainingBudget, 1200);

        // 5. Concurrent Fallback TCP Management Port Probes (SSH 22, HTTP 80, HTTPS 443, Telnet 23)
        List<CompletableFuture<PortProbeResult>> futures = new ArrayList<>();
        for (int port : MANAGEMENT_PORTS) {
            futures.add(CompletableFuture.supplyAsync(() -> probePort(targetIp, port, socketTimeout), probeExecutor));
        }

        List<PortProbeResult> results = new ArrayList<>();
        for (CompletableFuture<PortProbeResult> future : futures) {
            try {
                results.add(future.get(remainingBudget, TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (Exception e) {
                log.trace("Error executing port probe: {}", e.getMessage());
            }
        }

        // Analyze results: Prioritize established connections, then TCP resets (active host)
        PortProbeResult connected = results.stream()
                .filter(r -> "TCP_CONNECTION".equals(r.method))
                .findFirst()
                .orElse(null);

        if (connected != null) {
            return new DeviceReachabilityResponseDto(
                    deviceId, hostname, targetIp, true, "REACHABLE", "TCP_CONNECTION", connected.port, connected.responseTimeMs, Instant.now(),
                    "Target device confirmed reachable via TCP connection on port " + connected.port + " in " + connected.responseTimeMs + "ms."
            );
        }

        PortProbeResult reset = results.stream()
                .filter(r -> "TCP_RESET".equals(r.method))
                .findFirst()
                .orElse(null);

        if (reset != null) {
            return new DeviceReachabilityResponseDto(
                    deviceId, hostname, targetIp, true, "REACHABLE", "TCP_RESET", reset.port, reset.responseTimeMs, Instant.now(),
                    "Target device is active and routed (responded with TCP RST on port " + reset.port + " in " + reset.responseTimeMs + "ms)."
            );
        }

        // All probes timed out or refused
        long totalElapsed = System.currentTimeMillis() - startTime;
        return new DeviceReachabilityResponseDto(
                deviceId, hostname, targetIp, false, "UNREACHABLE", "NONE", null, null, Instant.now(),
                "Target device did not respond to ICMP echo or TCP probes on management ports [22, 80, 443, 23] within " + totalElapsed + "ms."
        );
    }

    private PortProbeResult probePort(String ip, int port, int timeoutMs) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            long latency = Math.max(1, System.currentTimeMillis() - start);
            return new PortProbeResult("TCP_CONNECTION", port, latency);
        } catch (ConnectException e) {
            long latency = Math.max(1, System.currentTimeMillis() - start);
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("refused") || msg.contains("rst")) {
                return new PortProbeResult("TCP_RESET", port, latency);
            }
            return new PortProbeResult("FAILED", port, null);
        } catch (Exception e) {
            return new PortProbeResult("FAILED", port, null);
        }
    }

    private boolean isValidTargetAddress(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        try {
            for (String part : parts) {
                int val = Integer.parseInt(part);
                if (val < 0 || val > 255) return false;
                if (part.length() > 1 && part.startsWith("0")) return false; // Reject leading zeros
            }
            // Disallow broadcast and unspecified targets
            return !ip.equals("0.0.0.0") && !ip.equals("255.255.255.255");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static class PortProbeResult {
        final String method;
        final int port;
        final Long responseTimeMs;

        PortProbeResult(String method, int port, Long responseTimeMs) {
            this.method = method;
            this.port = port;
            this.responseTimeMs = responseTimeMs;
        }
    }
}
