package com.nmap.repository;

import com.nmap.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NetworkInterfaceRepositoryTest {

    @Autowired
    private NetworkInterfaceRepository interfaceRepository;

    @Autowired
    private NetworkDeviceRepository deviceRepository;

    private NetworkDevice device;
    private NetworkInterface if1;

    @BeforeEach
    void setUp() {
        interfaceRepository.deleteAll();
        deviceRepository.deleteAll();

        device = new NetworkDevice("core-r1", "10.0.0.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router");
        device = deviceRepository.save(device);

        if1 = new NetworkInterface(device, "GigabitEthernet0/0/0", InterfaceType.GIGABIT_ETHERNET,
                "10.0.0.1", 24, "00:1A:2B:3C:4D:5E", AdminStatus.UP, OperationalStatus.UP, 1000L, "Uplink");
        if1 = interfaceRepository.save(if1);
    }

    @Test
    @DisplayName("Should find interfaces by device ID")
    void testFindByDeviceId() {
        List<NetworkInterface> interfaces = interfaceRepository.findByDeviceId(device.getId());
        assertThat(interfaces).hasSize(1);
        assertThat(interfaces.get(0).getInterfaceName()).isEqualTo("GigabitEthernet0/0/0");
    }

    @Test
    @DisplayName("Should check existence by device ID and interface name")
    void testExistsByDeviceIdAndInterfaceName() {
        assertThat(interfaceRepository.existsByDeviceIdAndInterfaceName(device.getId(), "GigabitEthernet0/0/0")).isTrue();
        assertThat(interfaceRepository.existsByDeviceIdAndInterfaceName(device.getId(), "FastEthernet0/1")).isFalse();
    }

    @Test
    @DisplayName("Should check existence excluding specific interface ID during update")
    void testExistsByDeviceIdAndInterfaceNameAndIdNot() {
        assertThat(interfaceRepository.existsByDeviceIdAndInterfaceNameAndIdNot(device.getId(), "GigabitEthernet0/0/0", if1.getId())).isFalse();
    }

    @Test
    @DisplayName("Should count interfaces by device ID")
    void testCountByDeviceId() {
        assertThat(interfaceRepository.countByDeviceId(device.getId())).isEqualTo(1);
    }
}
