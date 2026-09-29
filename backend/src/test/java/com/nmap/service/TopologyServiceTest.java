package com.nmap.service;

import com.nmap.dto.TopologyResponseDto;
import com.nmap.dto.TopologyResponseDto.TopologyLinkDto;
import com.nmap.dto.TopologyResponseDto.TopologyNodeDto;
import com.nmap.entity.*;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.impl.TopologyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopologyServiceTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private NetworkInterfaceRepository interfaceRepository;

    @Mock
    private NetworkLinkRepository linkRepository;

    @InjectMocks
    private TopologyServiceImpl topologyService;

    private NetworkDevice router;
    private NetworkDevice switchDevice;
    private NetworkInterface routerIface;
    private NetworkInterface switchIface;
    private NetworkLink link;

    @BeforeEach
    void setUp() {
        router = new NetworkDevice("core-rtr01", "192.168.10.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router");
        router.setId(1L);

        switchDevice = new NetworkDevice("dist-sw01", "192.168.10.2", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.UP, "Distribution Switch");
        switchDevice.setId(2L);

        routerIface = new NetworkInterface(router, "GigabitEthernet0/0/0", InterfaceType.GIGABIT_ETHERNET,
                "192.168.10.1", 24, "00:1A:2B:3C:4D:01", AdminStatus.UP, OperationalStatus.UP, 1000L, "Uplink to Switch");
        routerIface.setId(10L);

        switchIface = new NetworkInterface(switchDevice, "GigabitEthernet0/1", InterfaceType.GIGABIT_ETHERNET,
                "192.168.10.2", 24, "00:1A:2B:3C:4D:02", AdminStatus.UP, OperationalStatus.UP, 1000L, "Downlink to Router");
        switchIface.setId(20L);

        link = new NetworkLink(routerIface, switchIface, LinkStatus.UP, LinkType.ETHERNET, 1000L, "Trunk Link");
        link.setId(100L);
    }

    @Test
    @DisplayName("Should return empty topology when no devices or links exist")
    void testGetTopologyEmpty() {
        when(deviceRepository.findAll()).thenReturn(Collections.emptyList());
        when(linkRepository.findAll()).thenReturn(Collections.emptyList());

        TopologyResponseDto result = topologyService.getTopology();

        assertThat(result).isNotNull();
        assertThat(result.getTotalNodes()).isZero();
        assertThat(result.getTotalLinks()).isZero();
        assertThat(result.getNodes()).isEmpty();
        assertThat(result.getLinks()).isEmpty();

        verify(deviceRepository, times(1)).findAll();
        verify(linkRepository, times(1)).findAll();
        verifyNoInteractions(interfaceRepository);
    }

    @Test
    @DisplayName("Should return single device node with correct properties and zero links")
    void testGetTopologySingleDeviceWithoutLinks() {
        when(deviceRepository.findAll()).thenReturn(List.of(router));
        when(linkRepository.findAll()).thenReturn(Collections.emptyList());
        when(interfaceRepository.countByDeviceId(1L)).thenReturn(3L);

        TopologyResponseDto result = topologyService.getTopology();

        assertThat(result).isNotNull();
        assertThat(result.getTotalNodes()).isEqualTo(1);
        assertThat(result.getTotalLinks()).isZero();
        assertThat(result.getLinks()).isEmpty();
        assertThat(result.getNodes()).hasSize(1);

        TopologyNodeDto node = result.getNodes().get(0);
        assertThat(node.getId()).isEqualTo(1L);
        assertThat(node.getHostname()).isEqualTo("core-rtr01");
        assertThat(node.getManagementIp()).isEqualTo("192.168.10.1");
        assertThat(node.getDeviceType()).isEqualTo(DeviceType.ROUTER);
        assertThat(node.getVendor()).isEqualTo(DeviceVendor.CISCO);
        assertThat(node.getStatus()).isEqualTo(DeviceStatus.UP);
        assertThat(node.getInterfaceCount()).isEqualTo(3L);

        verify(deviceRepository, times(1)).findAll();
        verify(linkRepository, times(1)).findAll();
        verify(interfaceRepository, times(1)).countByDeviceId(1L);
    }

    @Test
    @DisplayName("Should return multiple connected devices and active link with full metadata")
    void testGetTopologyMultipleConnectedDevices() {
        when(deviceRepository.findAll()).thenReturn(List.of(router, switchDevice));
        when(linkRepository.findAll()).thenReturn(List.of(link));
        when(interfaceRepository.countByDeviceId(1L)).thenReturn(1L);
        when(interfaceRepository.countByDeviceId(2L)).thenReturn(4L);

        TopologyResponseDto result = topologyService.getTopology();

        assertThat(result).isNotNull();
        assertThat(result.getTotalNodes()).isEqualTo(2);
        assertThat(result.getTotalLinks()).isEqualTo(1);
        assertThat(result.getNodes()).hasSize(2);
        assertThat(result.getLinks()).hasSize(1);

        // Verify Node 1
        TopologyNodeDto node1 = result.getNodes().get(0);
        assertThat(node1.getId()).isEqualTo(1L);
        assertThat(node1.getHostname()).isEqualTo("core-rtr01");
        assertThat(node1.getInterfaceCount()).isEqualTo(1L);

        // Verify Node 2
        TopologyNodeDto node2 = result.getNodes().get(1);
        assertThat(node2.getId()).isEqualTo(2L);
        assertThat(node2.getHostname()).isEqualTo("dist-sw01");
        assertThat(node2.getInterfaceCount()).isEqualTo(4L);

        // Verify Link
        TopologyLinkDto linkDto = result.getLinks().get(0);
        assertThat(linkDto.getId()).isEqualTo(100L);
        assertThat(linkDto.getSourceDeviceId()).isEqualTo(1L);
        assertThat(linkDto.getSourceHostname()).isEqualTo("core-rtr01");
        assertThat(linkDto.getSourceInterfaceId()).isEqualTo(10L);
        assertThat(linkDto.getSourceInterfaceName()).isEqualTo("GigabitEthernet0/0/0");

        assertThat(linkDto.getDestinationDeviceId()).isEqualTo(2L);
        assertThat(linkDto.getDestinationHostname()).isEqualTo("dist-sw01");
        assertThat(linkDto.getDestinationInterfaceId()).isEqualTo(20L);
        assertThat(linkDto.getDestinationInterfaceName()).isEqualTo("GigabitEthernet0/1");

        assertThat(linkDto.getStatus()).isEqualTo(LinkStatus.UP);
        assertThat(linkDto.getLinkType()).isEqualTo(LinkType.ETHERNET);
        assertThat(linkDto.getBandwidthMbps()).isEqualTo(1000L);

        verify(interfaceRepository, times(1)).countByDeviceId(1L);
        verify(interfaceRepository, times(1)).countByDeviceId(2L);
    }

    @Test
    @DisplayName("Should correctly reflect node and link counts matching collection sizes")
    void testGetTopologyCountsMatchCollections() {
        when(deviceRepository.findAll()).thenReturn(List.of(router, switchDevice));
        when(linkRepository.findAll()).thenReturn(List.of(link));
        when(interfaceRepository.countByDeviceId(anyLong())).thenReturn(0L);

        TopologyResponseDto result = topologyService.getTopology();

        assertThat(result.getTotalNodes()).isEqualTo(result.getNodes().size());
        assertThat(result.getTotalLinks()).isEqualTo(result.getLinks().size());
    }

    @Test
    @DisplayName("Should report zero interface count when device has no interfaces")
    void testGetTopologyDeviceWithZeroInterfaces() {
        when(deviceRepository.findAll()).thenReturn(List.of(router));
        when(linkRepository.findAll()).thenReturn(Collections.emptyList());
        when(interfaceRepository.countByDeviceId(1L)).thenReturn(0L);

        TopologyResponseDto result = topologyService.getTopology();

        assertThat(result.getNodes()).hasSize(1);
        assertThat(result.getNodes().get(0).getInterfaceCount()).isZero();
    }

    @Test
    @DisplayName("Should handle edge case where link has null interface or device references gracefully")
    void testGetTopologyLinkWithNullEndpoints() {
        NetworkLink incompleteLink = new NetworkLink();
        incompleteLink.setId(500L);
        incompleteLink.setStatus(LinkStatus.DOWN);
        incompleteLink.setLinkType(LinkType.FIBER);
        incompleteLink.setBandwidthMbps(null);

        when(deviceRepository.findAll()).thenReturn(Collections.emptyList());
        when(linkRepository.findAll()).thenReturn(List.of(incompleteLink));

        TopologyResponseDto result = topologyService.getTopology();

        assertThat(result.getTotalNodes()).isZero();
        assertThat(result.getTotalLinks()).isEqualTo(1);
        assertThat(result.getLinks()).hasSize(1);

        TopologyLinkDto linkDto = result.getLinks().get(0);
        assertThat(linkDto.getId()).isEqualTo(500L);
        assertThat(linkDto.getSourceDeviceId()).isNull();
        assertThat(linkDto.getSourceHostname()).isNull();
        assertThat(linkDto.getSourceInterfaceId()).isNull();
        assertThat(linkDto.getSourceInterfaceName()).isNull();
        assertThat(linkDto.getDestinationDeviceId()).isNull();
        assertThat(linkDto.getDestinationHostname()).isNull();
        assertThat(linkDto.getDestinationInterfaceId()).isNull();
        assertThat(linkDto.getDestinationInterfaceName()).isNull();
        assertThat(linkDto.getStatus()).isEqualTo(LinkStatus.DOWN);
        assertThat(linkDto.getLinkType()).isEqualTo(LinkType.FIBER);
        assertThat(linkDto.getBandwidthMbps()).isNull();
    }
}
