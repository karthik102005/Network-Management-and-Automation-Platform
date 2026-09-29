package com.nmap.repository;

import com.nmap.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NetworkLinkRepositoryTest {

    @Autowired
    private NetworkLinkRepository linkRepository;

    @Autowired
    private NetworkInterfaceRepository interfaceRepository;

    @Autowired
    private NetworkDeviceRepository deviceRepository;

    private NetworkInterface if1;
    private NetworkInterface if2;
    private NetworkLink link;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        interfaceRepository.deleteAll();
        deviceRepository.deleteAll();

        NetworkDevice d1 = deviceRepository.save(new NetworkDevice("dev-a", "10.0.1.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Rtr A"));
        NetworkDevice d2 = deviceRepository.save(new NetworkDevice("dev-b", "10.0.2.1", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.UP, "Sw B"));

        if1 = interfaceRepository.save(new NetworkInterface(d1, "Gi0/0", InterfaceType.GIGABIT_ETHERNET, "10.0.0.1", 30, null, AdminStatus.UP, OperationalStatus.UP, 1000L, null));
        if2 = interfaceRepository.save(new NetworkInterface(d2, "Gi0/1", InterfaceType.GIGABIT_ETHERNET, "10.0.0.2", 30, null, AdminStatus.UP, OperationalStatus.UP, 1000L, null));

        link = linkRepository.save(new NetworkLink(if1, if2, LinkStatus.UP, LinkType.ETHERNET, 1000L, "Trunk Link"));
    }

    @Test
    @DisplayName("Should detect link between interfaces bidirectionally")
    void testExistsLinkBetweenInterfaces() {
        assertThat(linkRepository.existsLinkBetweenInterfaces(if1.getId(), if2.getId())).isTrue();
        assertThat(linkRepository.existsLinkBetweenInterfaces(if2.getId(), if1.getId())).isTrue();
    }

    @Test
    @DisplayName("Should find links by interface ID")
    void testFindByInterfaceId() {
        List<NetworkLink> links = linkRepository.findByInterfaceId(if1.getId());
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getDescription()).isEqualTo("Trunk Link");
    }

    @Test
    @DisplayName("Should filter links by status")
    void testFindByStatus() {
        List<NetworkLink> upLinks = linkRepository.findByStatus(LinkStatus.UP);
        assertThat(upLinks).hasSize(1);
    }
}
