package com.nmap.repository;

import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.NetworkDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NetworkDeviceRepositoryTest {

    @Autowired
    private NetworkDeviceRepository repository;

    private NetworkDevice r1;
    private NetworkDevice sw1;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        r1 = new NetworkDevice("R1", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router 1");
        sw1 = new NetworkDevice("SW1", "192.168.10.2", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.DOWN, "Distribution Switch");

        repository.save(r1);
        repository.save(sw1);
    }

    @Test
    @DisplayName("Should find device by exact hostname")
    void testFindByHostname() {
        Optional<NetworkDevice> found = repository.findByHostname("R1");
        assertThat(found).isPresent();
        assertThat(found.get().getManagementIp()).isEqualTo("192.168.10.1");
    }

    @Test
    @DisplayName("Should find device by management IP")
    void testFindByManagementIp() {
        Optional<NetworkDevice> found = repository.findByManagementIp("192.168.10.2");
        assertThat(found).isPresent();
        assertThat(found.get().getHostname()).isEqualTo("SW1");
    }

    @Test
    @DisplayName("Should check existence by hostname")
    void testExistsByHostname() {
        assertThat(repository.existsByHostname("R1")).isTrue();
        assertThat(repository.existsByHostname("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("Should check duplicate hostname excluding own ID during update")
    void testExistsByHostnameAndIdNot() {
        assertThat(repository.existsByHostnameAndIdNot("R1", r1.getId())).isFalse();
        assertThat(repository.existsByHostnameAndIdNot("SW1", r1.getId())).isTrue();
    }

    @Test
    @DisplayName("Should filter devices by device type")
    void testFindByDeviceType() {
        List<NetworkDevice> routers = repository.findByDeviceType(DeviceType.ROUTER);
        assertThat(routers).hasSize(1);
        assertThat(routers.get(0).getHostname()).isEqualTo("R1");
    }

    @Test
    @DisplayName("Should filter devices by vendor")
    void testFindByVendor() {
        List<NetworkDevice> ciscoDevices = repository.findByVendor(DeviceVendor.CISCO);
        assertThat(ciscoDevices).hasSize(2);
    }

    @Test
    @DisplayName("Should filter devices by operational status")
    void testFindByStatus() {
        List<NetworkDevice> upDevices = repository.findByStatus(DeviceStatus.UP);
        assertThat(upDevices).hasSize(1);
        assertThat(upDevices.get(0).getHostname()).isEqualTo("R1");
    }
}
