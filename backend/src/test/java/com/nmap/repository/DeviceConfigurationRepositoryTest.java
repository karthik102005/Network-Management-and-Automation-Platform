package com.nmap.repository;

import com.nmap.entity.ConfigFormat;
import com.nmap.entity.DeviceConfiguration;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.NetworkDevice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 8.1C: Repository test for DeviceConfigurationRepository, verifying persistence,
 * version ordering, active status filtering, device-level deletion, and unique constraints.
 */
@DataJpaTest
class DeviceConfigurationRepositoryTest {

    @Autowired
    private DeviceConfigurationRepository configurationRepository;

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
    @DisplayName("Should persist and retrieve a configuration snapshot with all attributes")
    void testSaveAndRetrieveConfigurationSnapshot() {
        NetworkDevice device = createDevice("rtr-repo-01", "10.10.1.1");

        DeviceConfiguration config = new DeviceConfiguration(
                device,
                1,
                "hostname rtr-repo-01\ninterface Gi0/0\n ip address 10.10.1.1 255.255.255.0\n",
                ConfigFormat.CISCO_IOS,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                true,
                "admin-user",
                "Initial baseline configuration"
        );

        DeviceConfiguration saved = configurationRepository.save(config);
        entityManager.flush();
        entityManager.clear();

        Optional<DeviceConfiguration> retrieved = configurationRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        DeviceConfiguration loaded = retrieved.get();

        assertThat(loaded.getVersion()).isEqualTo(1);
        assertThat(loaded.getConfigFormat()).isEqualTo(ConfigFormat.CISCO_IOS);
        assertThat(loaded.getConfigText()).contains("hostname rtr-repo-01");
        assertThat(loaded.getChecksum()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(loaded.getActive()).isTrue();
        assertThat(loaded.getAuthor()).isEqualTo("admin-user");
        assertThat(loaded.getDescription()).isEqualTo("Initial baseline configuration");
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getDevice().getId()).isEqualTo(device.getId());
    }

    @Test
    @DisplayName("Should retrieve configurations for a device ordered descending by version")
    void testFindByDeviceIdOrderByVersionDesc() {
        NetworkDevice device = createDevice("rtr-repo-02", "10.10.1.2");

        DeviceConfiguration v1 = new DeviceConfiguration(
                device, 1, "config v1", ConfigFormat.CISCO_IOS, "hash1", false, "admin", "v1"
        );
        DeviceConfiguration v2 = new DeviceConfiguration(
                device, 2, "config v2", ConfigFormat.CISCO_IOS, "hash2", false, "admin", "v2"
        );
        DeviceConfiguration v3 = new DeviceConfiguration(
                device, 3, "config v3", ConfigFormat.CISCO_IOS, "hash3", true, "admin", "v3"
        );

        configurationRepository.save(v1);
        configurationRepository.save(v3);
        configurationRepository.save(v2);
        entityManager.flush();
        entityManager.clear();

        List<DeviceConfiguration> configs = configurationRepository.findByDeviceIdOrderByVersionDesc(device.getId());
        assertThat(configs).hasSize(3);
        assertThat(configs.get(0).getVersion()).isEqualTo(3);
        assertThat(configs.get(1).getVersion()).isEqualTo(2);
        assertThat(configs.get(2).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find configuration by device ID and specific version")
    void testFindByDeviceIdAndVersion() {
        NetworkDevice device = createDevice("rtr-repo-03", "10.10.1.3");

        DeviceConfiguration v1 = new DeviceConfiguration(
                device, 1, "config v1", ConfigFormat.CISCO_IOS, "hash1", false, "admin", "v1"
        );
        DeviceConfiguration v2 = new DeviceConfiguration(
                device, 2, "config v2", ConfigFormat.CISCO_IOS, "hash2", true, "admin", "v2"
        );
        configurationRepository.save(v1);
        configurationRepository.save(v2);
        entityManager.flush();
        entityManager.clear();

        Optional<DeviceConfiguration> foundV2 = configurationRepository.findByDeviceIdAndVersion(device.getId(), 2);
        assertThat(foundV2).isPresent();
        assertThat(foundV2.get().getVersion()).isEqualTo(2);
        assertThat(foundV2.get().getConfigText()).isEqualTo("config v2");

        Optional<DeviceConfiguration> notFound = configurationRepository.findByDeviceIdAndVersion(device.getId(), 99);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find the top (highest) configuration version for a device")
    void testFindTopByDeviceIdOrderByVersionDesc() {
        NetworkDevice device = createDevice("rtr-repo-04", "10.10.1.4");

        DeviceConfiguration v1 = new DeviceConfiguration(
                device, 1, "config v1", ConfigFormat.CISCO_IOS, "hash1", false, "admin", "v1"
        );
        DeviceConfiguration v5 = new DeviceConfiguration(
                device, 5, "config v5", ConfigFormat.CISCO_IOS, "hash5", true, "admin", "v5"
        );
        DeviceConfiguration v3 = new DeviceConfiguration(
                device, 3, "config v3", ConfigFormat.CISCO_IOS, "hash3", false, "admin", "v3"
        );
        configurationRepository.save(v1);
        configurationRepository.save(v5);
        configurationRepository.save(v3);
        entityManager.flush();
        entityManager.clear();

        Optional<DeviceConfiguration> top = configurationRepository.findTopByDeviceIdOrderByVersionDesc(device.getId());
        assertThat(top).isPresent();
        assertThat(top.get().getVersion()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find active configuration for a device")
    void testFindByDeviceIdAndActiveTrue() {
        NetworkDevice device = createDevice("rtr-repo-05", "10.10.1.5");

        DeviceConfiguration v1 = new DeviceConfiguration(
                device, 1, "config v1", ConfigFormat.CISCO_IOS, "hash1", false, "admin", "v1"
        );
        DeviceConfiguration v2 = new DeviceConfiguration(
                device, 2, "config v2", ConfigFormat.CISCO_IOS, "hash2", true, "admin", "v2"
        );
        configurationRepository.save(v1);
        configurationRepository.save(v2);
        entityManager.flush();
        entityManager.clear();

        List<DeviceConfiguration> activeConfigs = configurationRepository.findByDeviceIdAndActiveTrue(device.getId());
        assertThat(activeConfigs).hasSize(1);
        assertThat(activeConfigs.get(0).getVersion()).isEqualTo(2);
        assertThat(activeConfigs.get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("Should delete all configurations for a specific device without affecting other devices")
    void testDeleteByDeviceId() {
        NetworkDevice deviceA = createDevice("rtr-repo-06a", "10.10.1.6");
        NetworkDevice deviceB = createDevice("rtr-repo-06b", "10.10.1.7");

        configurationRepository.save(new DeviceConfiguration(deviceA, 1, "cfgA-1", ConfigFormat.CISCO_IOS, "hA1", false, "adm", "desc"));
        configurationRepository.save(new DeviceConfiguration(deviceA, 2, "cfgA-2", ConfigFormat.CISCO_IOS, "hA2", true, "adm", "desc"));
        configurationRepository.save(new DeviceConfiguration(deviceB, 1, "cfgB-1", ConfigFormat.CISCO_IOS, "hB1", true, "adm", "desc"));
        entityManager.flush();
        entityManager.clear();

        configurationRepository.deleteByDeviceId(deviceA.getId());
        entityManager.flush();
        entityManager.clear();

        List<DeviceConfiguration> configsA = configurationRepository.findByDeviceIdOrderByVersionDesc(deviceA.getId());
        assertThat(configsA).isEmpty();

        List<DeviceConfiguration> configsB = configurationRepository.findByDeviceIdOrderByVersionDesc(deviceB.getId());
        assertThat(configsB).hasSize(1);
        assertThat(configsB.get(0).getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should enforce unique constraint on device ID and version")
    void testUniqueConstraintOnDeviceIdAndVersion() {
        NetworkDevice device = createDevice("rtr-repo-07", "10.10.1.8");

        DeviceConfiguration v1 = new DeviceConfiguration(
                device, 1, "config v1", ConfigFormat.CISCO_IOS, "hash1", true, "admin", "desc"
        );
        configurationRepository.saveAndFlush(v1);

        DeviceConfiguration duplicateV1 = new DeviceConfiguration(
                device, 1, "config v1 duplicate", ConfigFormat.CISCO_IOS, "hash2", true, "admin2", "desc2"
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            configurationRepository.saveAndFlush(duplicateV1);
        });
    }
}
