package com.nmap.repository;

import com.nmap.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8.1C: Repository test for NetworkAlertRepository, verifying alert persistence,
 * status/severity filtering, active alert collections, deduplication queries,
 * lifecycle timestamps, and device-level deletion.
 */
@DataJpaTest
class NetworkAlertRepositoryTest {

    @Autowired
    private NetworkAlertRepository alertRepository;

    @Autowired
    private NetworkDeviceRepository deviceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private NetworkDevice createDevice(String hostname, String ip) {
        NetworkDevice device = new NetworkDevice(
                hostname, ip, DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Test Router " + hostname
        );
        return deviceRepository.save(device);
    }

    @Test
    @DisplayName("Should persist and retrieve a network alert with all attributes")
    void testSaveAndRetrieveNetworkAlert() {
        NetworkDevice device = createDevice("rtr-alert-01", "10.30.1.1");

        NetworkAlert alert = new NetworkAlert(
                device,
                AlertType.DEVICE_UNREACHABLE,
                AlertSeverity.CRITICAL,
                AlertStatus.OPEN,
                "Device is unreachable via ICMP echo and management ports.",
                "REACHABILITY_MONITOR"
        );

        NetworkAlert saved = alertRepository.save(alert);
        entityManager.flush();
        entityManager.clear();

        Optional<NetworkAlert> retrieved = alertRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        NetworkAlert loaded = retrieved.get();

        assertThat(loaded.getAlertType()).isEqualTo(AlertType.DEVICE_UNREACHABLE);
        assertThat(loaded.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(loaded.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(loaded.getSource()).isEqualTo("REACHABILITY_MONITOR");
        assertThat(loaded.getMessage()).contains("Device is unreachable");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getAcknowledgedAt()).isNull();
        assertThat(loaded.getResolvedAt()).isNull();
        assertThat(loaded.getDevice().getId()).isEqualTo(device.getId());
    }

    @Test
    @DisplayName("Should retrieve alerts by device ID ordered descending by creation time")
    void testFindByDeviceIdOrderByCreatedAtDesc() {
        NetworkDevice device = createDevice("rtr-alert-02", "10.30.1.2");

        Instant t1 = Instant.now().minusSeconds(120);
        Instant t2 = Instant.now().minusSeconds(60);
        Instant t3 = Instant.now();

        NetworkAlert alert1 = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Alert 1", "SRC");
        alert1.setCreatedAt(t1);

        NetworkAlert alert2 = new NetworkAlert(device, AlertType.HIGH_LATENCY, AlertSeverity.HIGH, AlertStatus.OPEN, "Alert 2", "SRC");
        alert2.setCreatedAt(t2);

        NetworkAlert alert3 = new NetworkAlert(device, AlertType.INTERFACE_DOWN, AlertSeverity.HIGH, AlertStatus.OPEN, "Alert 3", "SRC");
        alert3.setCreatedAt(t3);

        alertRepository.save(alert1);
        alertRepository.save(alert3);
        alertRepository.save(alert2);
        entityManager.flush();
        entityManager.clear();

        List<NetworkAlert> alerts = alertRepository.findByDeviceIdOrderByCreatedAtDesc(device.getId());
        assertThat(alerts).hasSize(3);
        assertThat(alerts.get(0).getMessage()).isEqualTo("Alert 3");
        assertThat(alerts.get(1).getMessage()).isEqualTo("Alert 2");
        assertThat(alerts.get(2).getMessage()).isEqualTo("Alert 1");

        // Verify standard findByDeviceId as well
        List<NetworkAlert> unconstrained = alertRepository.findByDeviceId(device.getId());
        assertThat(unconstrained).hasSize(3);
    }

    @Test
    @DisplayName("Should filter alerts by status and severity ordered by creation time")
    void testFilterByStatusAndSeverity() {
        NetworkDevice device = createDevice("rtr-alert-03", "10.30.1.3");

        NetworkAlert openCrit = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Open Critical", "SRC");
        NetworkAlert ackHigh = new NetworkAlert(device, AlertType.INTERFACE_DOWN, AlertSeverity.HIGH, AlertStatus.ACKNOWLEDGED, "Ack High", "SRC");
        NetworkAlert resCrit = new NetworkAlert(device, AlertType.LINK_DOWN, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, "Resolved Critical", "SRC");

        alertRepository.save(openCrit);
        alertRepository.save(ackHigh);
        alertRepository.save(resCrit);
        entityManager.flush();
        entityManager.clear();

        List<NetworkAlert> openAlerts = alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.OPEN);
        assertThat(openAlerts).extracting(NetworkAlert::getStatus).containsOnly(AlertStatus.OPEN);
        assertThat(openAlerts).anyMatch(a -> a.getMessage().equals("Open Critical"));

        List<NetworkAlert> criticalAlerts = alertRepository.findBySeverityOrderByCreatedAtDesc(AlertSeverity.CRITICAL);
        assertThat(criticalAlerts).extracting(NetworkAlert::getSeverity).containsOnly(AlertSeverity.CRITICAL);
        assertThat(criticalAlerts).anyMatch(a -> a.getMessage().equals("Open Critical"));
        assertThat(criticalAlerts).anyMatch(a -> a.getMessage().equals("Resolved Critical"));
    }

    @Test
    @DisplayName("Should retrieve alerts using a collection of statuses and exclude resolved alerts")
    void testFindByDeviceIdAndStatusIn_ActiveAlerts() {
        NetworkDevice device = createDevice("rtr-alert-04", "10.30.1.4");

        NetworkAlert openAlert = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Open alert", "SRC");
        NetworkAlert ackAlert = new NetworkAlert(device, AlertType.HIGH_LATENCY, AlertSeverity.MEDIUM, AlertStatus.ACKNOWLEDGED, "Ack alert", "SRC");
        NetworkAlert resolvedAlert = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, "Resolved alert", "SRC");
        resolvedAlert.setResolvedAt(Instant.now());

        alertRepository.save(openAlert);
        alertRepository.save(ackAlert);
        alertRepository.save(resolvedAlert);
        entityManager.flush();
        entityManager.clear();

        List<NetworkAlert> activeAlerts = alertRepository.findByDeviceIdAndStatusIn(
                device.getId(),
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );

        assertThat(activeAlerts).hasSize(2);
        assertThat(activeAlerts).extracting(NetworkAlert::getStatus)
                .containsExactlyInAnyOrder(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);
        assertThat(activeAlerts).extracting(NetworkAlert::getMessage)
                .doesNotContain("Resolved alert");
    }

    @Test
    @DisplayName("Should find latest matching active alert for deduplication and return empty when none active")
    void testFindFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc_Deduplication() {
        NetworkDevice device = createDevice("rtr-alert-05", "10.30.1.5");

        // Case 1: When no alerts exist
        Optional<NetworkAlert> noneFound = alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                device.getId(),
                AlertType.DEVICE_UNREACHABLE,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        assertThat(noneFound).isEmpty();

        // Case 2: When only RESOLVED alert exists
        NetworkAlert resolved = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.RESOLVED, "Old resolved", "SRC");
        resolved.setCreatedAt(Instant.now().minusSeconds(300));
        resolved.setResolvedAt(Instant.now().minusSeconds(100));
        alertRepository.save(resolved);
        entityManager.flush();
        entityManager.clear();

        Optional<NetworkAlert> stillEmpty = alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                device.getId(),
                AlertType.DEVICE_UNREACHABLE,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        assertThat(stillEmpty).isEmpty();

        // Case 3: When multiple active alerts exist, return the newest
        NetworkAlert activeOlder = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.ACKNOWLEDGED, "Active Older", "SRC");
        activeOlder.setCreatedAt(Instant.now().minusSeconds(120));

        NetworkAlert activeNewer = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Active Newer", "SRC");
        activeNewer.setCreatedAt(Instant.now().minusSeconds(10));

        alertRepository.save(activeOlder);
        alertRepository.save(activeNewer);
        entityManager.flush();
        entityManager.clear();

        Optional<NetworkAlert> latestActive = alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                device.getId(),
                AlertType.DEVICE_UNREACHABLE,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        assertThat(latestActive).isPresent();
        assertThat(latestActive.get().getMessage()).isEqualTo("Active Newer");

        // Case 4: Query for a different alert type returns empty
        Optional<NetworkAlert> interfaceAlert = alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                device.getId(),
                AlertType.INTERFACE_DOWN,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
        );
        assertThat(interfaceAlert).isEmpty();
    }

    @Test
    @DisplayName("Should persist acknowledgement and resolution timestamps accurately")
    void testPersistAcknowledgementAndResolutionTimestamps() {
        NetworkDevice device = createDevice("rtr-alert-06", "10.30.1.6");

        NetworkAlert alert = new NetworkAlert(device, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Probe failed", "REACHABILITY_MONITOR");
        NetworkAlert saved = alertRepository.save(alert);
        entityManager.flush();

        // Acknowledge alert
        Instant ackTime = Instant.now();
        saved.setStatus(AlertStatus.ACKNOWLEDGED);
        saved.setAcknowledgedAt(ackTime);
        alertRepository.save(saved);
        entityManager.flush();

        NetworkAlert ackLoaded = alertRepository.findById(saved.getId()).orElseThrow();
        assertThat(ackLoaded.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(ackLoaded.getAcknowledgedAt()).isNotNull();
        assertThat(ackLoaded.getResolvedAt()).isNull();

        // Resolve alert
        Instant resTime = Instant.now();
        ackLoaded.setStatus(AlertStatus.RESOLVED);
        ackLoaded.setResolvedAt(resTime);
        ackLoaded.setMessage(ackLoaded.getMessage() + " [Auto-recovered: Device reachable]");
        alertRepository.save(ackLoaded);
        entityManager.flush();
        entityManager.clear();

        NetworkAlert resLoaded = alertRepository.findById(saved.getId()).orElseThrow();
        assertThat(resLoaded.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(resLoaded.getAcknowledgedAt()).isNotNull();
        assertThat(resLoaded.getResolvedAt()).isNotNull();
        assertThat(resLoaded.getMessage()).contains("[Auto-recovered: Device reachable]");
    }

    @Test
    @DisplayName("Should delete alerts for a specific device without affecting other devices")
    void testDeleteByDeviceId() {
        NetworkDevice deviceA = createDevice("rtr-alert-07a", "10.30.1.7");
        NetworkDevice deviceB = createDevice("rtr-alert-07b", "10.30.1.8");

        alertRepository.save(new NetworkAlert(deviceA, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Alert A1", "SRC"));
        alertRepository.save(new NetworkAlert(deviceA, AlertType.HIGH_LATENCY, AlertSeverity.HIGH, AlertStatus.OPEN, "Alert A2", "SRC"));
        alertRepository.save(new NetworkAlert(deviceB, AlertType.DEVICE_UNREACHABLE, AlertSeverity.CRITICAL, AlertStatus.OPEN, "Alert B1", "SRC"));
        entityManager.flush();
        entityManager.clear();

        alertRepository.deleteByDeviceId(deviceA.getId());
        entityManager.flush();
        entityManager.clear();

        List<NetworkAlert> alertsA = alertRepository.findByDeviceId(deviceA.getId());
        assertThat(alertsA).isEmpty();

        List<NetworkAlert> alertsB = alertRepository.findByDeviceId(deviceB.getId());
        assertThat(alertsB).hasSize(1);
        assertThat(alertsB.get(0).getMessage()).isEqualTo("Alert B1");
    }
}
