package com.nmap.network;

import com.nmap.dto.DeviceReachabilityResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceReachabilityServiceTest {

    private DeviceReachabilityService reachabilityService;

    @BeforeEach
    void setUp() {
        reachabilityService = new DeviceReachabilityService();
    }

    @AfterEach
    void tearDown() {
        reachabilityService.cleanup();
    }

    @Test
    @DisplayName("Should reject null or blank IP addresses as INVALID_TARGET without socket operations")
    void testBlankOrNullIp() {
        DeviceReachabilityResponseDto nullResult = reachabilityService.checkReachability(1L, "dev1", null, 1000);
        assertThat(nullResult.isReachable()).isFalse();
        assertThat(nullResult.getReachabilityStatus()).isEqualTo("INVALID_TARGET");
        assertThat(nullResult.getProbeMethod()).isEqualTo("NONE");
        assertThat(nullResult.getResponseTimeMs()).isNull();

        DeviceReachabilityResponseDto blankResult = reachabilityService.checkReachability(1L, "dev1", "   ", 1000);
        assertThat(blankResult.isReachable()).isFalse();
        assertThat(blankResult.getReachabilityStatus()).isEqualTo("INVALID_TARGET");
    }

    @Test
    @DisplayName("Should reject malformed IP addresses as INVALID_TARGET without network probe")
    void testInvalidIpFormat() {
        DeviceReachabilityResponseDto invalidResult = reachabilityService.checkReachability(1L, "dev1", "999.999.999.999", 1000);
        assertThat(invalidResult.isReachable()).isFalse();
        assertThat(invalidResult.getReachabilityStatus()).isEqualTo("INVALID_TARGET");
        assertThat(invalidResult.getDetails()).contains("invalid");

        DeviceReachabilityResponseDto alphaResult = reachabilityService.checkReachability(1L, "dev1", "not-an-ip", 1000);
        assertThat(alphaResult.isReachable()).isFalse();
        assertThat(alphaResult.getReachabilityStatus()).isEqualTo("INVALID_TARGET");
    }

    @Test
    @DisplayName("Should reject broadcast and zero network addresses as INVALID_TARGET")
    void testBroadcastAndZeroAddresses() {
        DeviceReachabilityResponseDto broadcastResult = reachabilityService.checkReachability(1L, "dev1", "255.255.255.255", 1000);
        assertThat(broadcastResult.isReachable()).isFalse();
        assertThat(broadcastResult.getReachabilityStatus()).isEqualTo("INVALID_TARGET");

        DeviceReachabilityResponseDto zeroResult = reachabilityService.checkReachability(1L, "dev1", "0.0.0.0", 1000);
        assertThat(zeroResult.isReachable()).isFalse();
        assertThat(zeroResult.getReachabilityStatus()).isEqualTo("INVALID_TARGET");
    }

    @Test
    @DisplayName("Should test loopback address safely and return structured diagnostic result")
    void testLoopbackReachability() {
        // Probing 127.0.0.1 (local loopback) is permitted by constraint 6
        DeviceReachabilityResponseDto result = reachabilityService.checkReachability(1L, "localhost", "127.0.0.1", 1000);
        assertThat(result.getDeviceId()).isEqualTo(1L);
        assertThat(result.getHostname()).isEqualTo("localhost");
        assertThat(result.getManagementIp()).isEqualTo("127.0.0.1");
        assertThat(result.getCheckedAt()).isNotNull();
        assertThat(result.getDetails()).isNotBlank();
        // On loopback, target will either succeed via ICMP or TCP probe or report status safely
        assertThat(result.getReachabilityStatus()).isIn("REACHABLE", "UNREACHABLE");
    }

    @Test
    @DisplayName("Should enforce bounded timeout budget between MIN and MAX constants")
    void testTimeoutBudgetConstants() {
        assertThat(DeviceReachabilityService.MIN_TIMEOUT_MS).isEqualTo(500);
        assertThat(DeviceReachabilityService.MAX_TIMEOUT_MS).isEqualTo(3000);
        assertThat(DeviceReachabilityService.DEFAULT_TIMEOUT_MS).isEqualTo(2000);
    }

    @Test
    @DisplayName("Backwards compatibility: isReachable and isPortOpen helper methods")
    void testLegacyHelperMethods() {
        assertThat(reachabilityService.isReachable(null, 500)).isFalse();
        assertThat(reachabilityService.isReachable("  ", 500)).isFalse();
        // Probing a port that is likely closed on localhost returns false safely without throwing exception
        assertThat(reachabilityService.isPortOpen("127.0.0.1", 65530, 200)).isFalse();
    }
}
