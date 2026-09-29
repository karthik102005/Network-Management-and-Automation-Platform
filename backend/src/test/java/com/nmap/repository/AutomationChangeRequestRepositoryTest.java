package com.nmap.repository;

import com.nmap.entity.AutomationChangeRequest;
import com.nmap.entity.AutomationStatus;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.NetworkDevice;
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
 * Phase 8.1C: Repository test for AutomationChangeRequestRepository, verifying persistence,
 * descending creation-time ordering, status transitions, rollback details, and failure reporting.
 */
@DataJpaTest
class AutomationChangeRequestRepositoryTest {

    @Autowired
    private AutomationChangeRequestRepository changeRequestRepository;

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
    @DisplayName("Should persist and retrieve a draft change request with defaults and audit fields")
    void testSaveAndRetrieveDraftChangeRequest() {
        NetworkDevice device = createDevice("rtr-auto-01", "10.20.1.1");

        AutomationChangeRequest request = new AutomationChangeRequest(
                device,
                "Configure NTP Baseline",
                "Ensure NTP synchronized across all edge routers",
                "playbook-ntp-syslog",
                "ntp server pool.ntp.org\nservice timestamps log datetime msec\n",
                "operator-karthik",
                1
        );

        AutomationChangeRequest saved = changeRequestRepository.save(request);
        entityManager.flush();
        entityManager.clear();

        Optional<AutomationChangeRequest> retrieved = changeRequestRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        AutomationChangeRequest loaded = retrieved.get();

        assertThat(loaded.getTitle()).isEqualTo("Configure NTP Baseline");
        assertThat(loaded.getDescription()).isEqualTo("Ensure NTP synchronized across all edge routers");
        assertThat(loaded.getPlaybookId()).isEqualTo("playbook-ntp-syslog");
        assertThat(loaded.getConfigCommands()).contains("ntp server pool.ntp.org");
        assertThat(loaded.getAuthor()).isEqualTo("operator-karthik");
        assertThat(loaded.getPreChangeVersion()).isEqualTo(1);
        assertThat(loaded.getStatus()).isEqualTo(AutomationStatus.DRAFT);
        assertThat(loaded.getIsSimulated()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getExecutedAt()).isNull();
        assertThat(loaded.getCompletedAt()).isNull();
        assertThat(loaded.getDevice().getId()).isEqualTo(device.getId());
    }

    @Test
    @DisplayName("Should retrieve requests for a specific device in descending creation order")
    void testFindByDeviceIdOrderByCreatedAtDesc() {
        NetworkDevice device = createDevice("rtr-auto-02", "10.20.1.2");

        Instant t1 = Instant.now().minusSeconds(120);
        Instant t2 = Instant.now().minusSeconds(60);
        Instant t3 = Instant.now();

        AutomationChangeRequest req1 = new AutomationChangeRequest(device, "CR1", "desc1", "pb1", "cmd1", "author", 1);
        req1.setCreatedAt(t1);

        AutomationChangeRequest req2 = new AutomationChangeRequest(device, "CR2", "desc2", "pb2", "cmd2", "author", 2);
        req2.setCreatedAt(t2);

        AutomationChangeRequest req3 = new AutomationChangeRequest(device, "CR3", "desc3", "pb3", "cmd3", "author", 3);
        req3.setCreatedAt(t3);

        changeRequestRepository.save(req1);
        changeRequestRepository.save(req3);
        changeRequestRepository.save(req2);
        entityManager.flush();
        entityManager.clear();

        List<AutomationChangeRequest> requests = changeRequestRepository.findByDeviceIdOrderByCreatedAtDesc(device.getId());
        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).getTitle()).isEqualTo("CR3");
        assertThat(requests.get(1).getTitle()).isEqualTo("CR2");
        assertThat(requests.get(2).getTitle()).isEqualTo("CR1");
    }

    @Test
    @DisplayName("Should retrieve all requests across devices in descending creation order")
    void testFindAllByOrderByCreatedAtDesc() {
        NetworkDevice deviceA = createDevice("rtr-auto-03a", "10.20.1.3");
        NetworkDevice deviceB = createDevice("rtr-auto-03b", "10.20.1.4");

        Instant t1 = Instant.now().minusSeconds(180);
        Instant t2 = Instant.now().minusSeconds(120);
        Instant t3 = Instant.now().minusSeconds(60);

        AutomationChangeRequest reqA1 = new AutomationChangeRequest(deviceA, "A-Oldest", "desc", "pb", "cmd", "auth", 1);
        reqA1.setCreatedAt(t1);

        AutomationChangeRequest reqB = new AutomationChangeRequest(deviceB, "B-Middle", "desc", "pb", "cmd", "auth", 1);
        reqB.setCreatedAt(t2);

        AutomationChangeRequest reqA2 = new AutomationChangeRequest(deviceA, "A-Newest", "desc", "pb", "cmd", "auth", 2);
        reqA2.setCreatedAt(t3);

        changeRequestRepository.save(reqA1);
        changeRequestRepository.save(reqA2);
        changeRequestRepository.save(reqB);
        entityManager.flush();
        entityManager.clear();

        List<AutomationChangeRequest> all = changeRequestRepository.findAllByOrderByCreatedAtDesc();
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);

        // Verify strictly descending order
        for (int i = 0; i < all.size() - 1; i++) {
            assertThat(all.get(i).getCreatedAt()).isAfterOrEqualTo(all.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("Should persist valid status transitions and version references from DRAFT to COMPLETED")
    void testPersistStatusAndVersionTransitions() {
        NetworkDevice device = createDevice("rtr-auto-04", "10.20.1.5");

        AutomationChangeRequest request = new AutomationChangeRequest(
                device, "ACL Update", "Apply ACL", "playbook-acl", "ip access-list standard 10", "neteng", 2
        );
        AutomationChangeRequest saved = changeRequestRepository.save(request);
        entityManager.flush();

        // 1. Transition to EXECUTING
        saved.setStatus(AutomationStatus.EXECUTING);
        saved.setExecutedAt(Instant.now());
        changeRequestRepository.save(saved);
        entityManager.flush();

        // 2. Transition to COMPLETED with post-change version
        saved.setStatus(AutomationStatus.COMPLETED);
        saved.setPostChangeVersion(3);
        saved.setCompletedAt(Instant.now());
        saved.setExecutionLog("Pre-check PASSED. Syntax PASSED. Committed v3.");
        saved.setTerminalTranscript("R1# configure terminal\nBuilding configuration...\n[OK]");
        changeRequestRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        AutomationChangeRequest loaded = changeRequestRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(AutomationStatus.COMPLETED);
        assertThat(loaded.getPreChangeVersion()).isEqualTo(2);
        assertThat(loaded.getPostChangeVersion()).isEqualTo(3);
        assertThat(loaded.getExecutedAt()).isNotNull();
        assertThat(loaded.getCompletedAt()).isNotNull();
        assertThat(loaded.getExecutionLog()).contains("Committed v3");
        assertThat(loaded.getTerminalTranscript()).contains("[OK]");
    }

    @Test
    @DisplayName("Should persist rollback-related information and state transition to ROLLED_BACK")
    void testPersistRollbackInformation() {
        NetworkDevice device = createDevice("rtr-auto-05", "10.20.1.6");

        AutomationChangeRequest request = new AutomationChangeRequest(
                device, "VLAN Change", "Change VLAN", "playbook-vlan", "vlan 100", "operator", 1
        );
        request.setStatus(AutomationStatus.COMPLETED);
        request.setPostChangeVersion(2);
        AutomationChangeRequest saved = changeRequestRepository.save(request);
        entityManager.flush();

        // Rollback operation
        saved.setStatus(AutomationStatus.ROLLED_BACK);
        saved.setRollbackVersion(3); // Non-destructive rollback creates v3 restoring v1
        saved.setExecutionLog("Change rolled back: Restored baseline configuration v1 as active v3");
        saved.setTerminalTranscript("Rolling back change: Applied configuration snapshot from v1");
        changeRequestRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        AutomationChangeRequest loaded = changeRequestRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(AutomationStatus.ROLLED_BACK);
        assertThat(loaded.getPreChangeVersion()).isEqualTo(1);
        assertThat(loaded.getPostChangeVersion()).isEqualTo(2);
        assertThat(loaded.getRollbackVersion()).isEqualTo(3);
        assertThat(loaded.getExecutionLog()).contains("Restored baseline configuration v1 as active v3");
    }

    @Test
    @DisplayName("Should persist failure details, error messages, and execution logs")
    void testPersistFailureAndErrorMessage() {
        NetworkDevice device = createDevice("rtr-auto-06", "10.20.1.7");

        AutomationChangeRequest request = new AutomationChangeRequest(
                device, "Dangerous Command", "Invalid change", "playbook-custom", "reload in 0\n", "unauthorized", 1
        );
        AutomationChangeRequest saved = changeRequestRepository.save(request);
        entityManager.flush();

        // Execution failure
        saved.setStatus(AutomationStatus.FAILED);
        saved.setErrorMessage("Syntax validation rejected prohibited command: 'reload in 0'");
        saved.setExecutionLog("Pre-check PASSED. Syntax validation FAILED. Execution aborted.");
        changeRequestRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        AutomationChangeRequest loaded = changeRequestRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(AutomationStatus.FAILED);
        assertThat(loaded.getErrorMessage()).contains("Syntax validation rejected prohibited command");
        assertThat(loaded.getExecutionLog()).contains("Syntax validation FAILED");
    }
}
