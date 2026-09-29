package com.nmap.service.impl;

import com.nmap.dto.*;
import com.nmap.entity.NetworkDevice;
import com.nmap.entity.NetworkInterface;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.service.DeviceMetricsProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic simulated telemetry provider.
 * <p>
 * Generates synthetic CPU, memory, latency, and interface utilization metrics
 * for educational demonstration and lab verification. Does NOT poll physical SNMP OIDs
 * and does NOT mutate persistent device inventory status.
 */
@Service
public class SimulatedDeviceMetricsProvider implements DeviceMetricsProvider {

    private static final String TELEMETRY_SOURCE = "SIMULATED_PROVIDER (Educational/Lab Prototype)";
    private static final String DISCLAIMER = "Simulated telemetry data for educational and development demonstration. " +
            "Not collected via physical SNMP OID polling.";

    private final NetworkDeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;

    public SimulatedDeviceMetricsProvider(NetworkDeviceRepository deviceRepository,
                                          NetworkInterfaceRepository interfaceRepository) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceMetricsDto getDeviceMetrics(Long deviceId) {
        NetworkDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", deviceId));

        Instant now = Instant.now();
        long epochSec = now.getEpochSecond();

        double cpu = computeDeterministicCpu(deviceId, epochSec);
        double memory = computeDeterministicMemory(deviceId, epochSec);
        double latency = computeDeterministicLatency(deviceId, epochSec);
        double packetLoss = 0.0;

        DeviceHealthState healthState = evaluateHealth(cpu, memory, latency);

        List<NetworkInterface> interfaces = interfaceRepository.findByDeviceId(deviceId);
        List<InterfaceMetricsDto> interfaceMetrics = new ArrayList<>();

        if (interfaces.isEmpty()) {
            interfaceMetrics.add(new InterfaceMetricsDto("Mgmt0", 12.5, 24.8, 1000, 2.48));
        } else {
            for (NetworkInterface iface : interfaces) {
                long capacity = (iface.getSpeedMbps() != null && iface.getSpeedMbps() > 0)
                        ? iface.getSpeedMbps()
                        : 1000L;
                double inMbps = roundOneDecimal(15.0 + ((deviceId * 11 + iface.getId() * 7) % 35) + 8.0 * Math.sin(epochSec / 120.0));
                double outMbps = roundOneDecimal(25.0 + ((deviceId * 13 + iface.getId() * 5) % 50) + 12.0 * Math.cos(epochSec / 120.0));
                double util = roundOneDecimal(Math.max(inMbps, outMbps) / capacity * 100.0);

                interfaceMetrics.add(new InterfaceMetricsDto(
                        iface.getInterfaceName(),
                        inMbps,
                        outMbps,
                        capacity,
                        util
                ));
            }
        }

        return new DeviceMetricsDto(
                device.getId(),
                device.getHostname(),
                now,
                cpu,
                memory,
                latency,
                packetLoss,
                healthState,
                interfaceMetrics,
                true,
                TELEMETRY_SOURCE,
                DISCLAIMER
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceMetricsHistoryPointDto> getDeviceMetricsHistory(Long deviceId, int points) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("NetworkDevice", "id", deviceId);
        }

        if (points < 1 || points > 60) {
            throw new IllegalArgumentException("History points count must be between 1 and 60, requested: " + points);
        }

        Instant now = Instant.now();
        List<DeviceMetricsHistoryPointDto> history = new ArrayList<>(points);

        // Generate ordered ascending points spaced 60 seconds apart
        for (int i = points - 1; i >= 0; i--) {
            Instant ptTime = now.minus(i * 60L, ChronoUnit.SECONDS);
            long epochSec = ptTime.getEpochSecond();

            double cpu = computeDeterministicCpu(deviceId, epochSec);
            double memory = computeDeterministicMemory(deviceId, epochSec);
            double latency = computeDeterministicLatency(deviceId, epochSec);
            double inMbps = roundOneDecimal(20.0 + ((deviceId * 9) % 30) + 10.0 * Math.sin(epochSec / 180.0));
            double outMbps = roundOneDecimal(35.0 + ((deviceId * 7) % 45) + 15.0 * Math.cos(epochSec / 180.0));

            history.add(new DeviceMetricsHistoryPointDto(
                    ptTime,
                    cpu,
                    memory,
                    latency,
                    inMbps,
                    outMbps
            ));
        }

        return history;
    }

    private double computeDeterministicCpu(Long deviceId, long epochSec) {
        double base = 25.0 + ((deviceId * 7) % 25);
        double wave = 12.0 * Math.sin(epochSec / 150.0);
        return roundOneDecimal(Math.max(5.0, Math.min(95.0, base + wave)));
    }

    private double computeDeterministicMemory(Long deviceId, long epochSec) {
        double base = 42.0 + ((deviceId * 5) % 20);
        double wave = 6.0 * Math.cos(epochSec / 300.0);
        return roundOneDecimal(Math.max(10.0, Math.min(90.0, base + wave)));
    }

    private double computeDeterministicLatency(Long deviceId, long epochSec) {
        double base = 8.0 + ((deviceId * 3) % 12);
        double wave = 4.0 * Math.sin(epochSec / 90.0);
        return roundOneDecimal(Math.max(1.0, Math.min(80.0, base + wave)));
    }

    private DeviceHealthState evaluateHealth(double cpu, double memory, double latency) {
        if (cpu > 85.0 || memory > 85.0 || latency > 100.0) {
            return DeviceHealthState.DEGRADED;
        } else if (cpu > 70.0 || memory > 75.0 || latency > 50.0) {
            return DeviceHealthState.WARNING;
        }
        return DeviceHealthState.HEALTHY;
    }

    private double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
