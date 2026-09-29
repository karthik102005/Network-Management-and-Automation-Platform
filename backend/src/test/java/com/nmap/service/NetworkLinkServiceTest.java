package com.nmap.service;

import com.nmap.dto.LinkRequestDto;
import com.nmap.dto.LinkResponseDto;
import com.nmap.entity.*;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.impl.NetworkLinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkLinkServiceTest {

    @Mock
    private NetworkLinkRepository linkRepository;

    @Mock
    private NetworkInterfaceRepository interfaceRepository;

    @InjectMocks
    private NetworkLinkServiceImpl linkService;

    private NetworkInterface srcIf;
    private NetworkInterface dstIf;
    private NetworkLink sampleLink;
    private LinkRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        NetworkDevice d1 = new NetworkDevice("r1", "10.0.0.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "R1");
        d1.setId(1L);
        NetworkDevice d2 = new NetworkDevice("sw1", "10.0.0.2", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.UP, "SW1");
        d2.setId(2L);

        srcIf = new NetworkInterface(d1, "Gi0/0", InterfaceType.GIGABIT_ETHERNET, "10.0.0.1", 30, null, AdminStatus.UP, OperationalStatus.UP, 1000L, null);
        srcIf.setId(10L);

        dstIf = new NetworkInterface(d2, "Gi0/1", InterfaceType.GIGABIT_ETHERNET, "10.0.0.2", 30, null, AdminStatus.UP, OperationalStatus.UP, 1000L, null);
        dstIf.setId(20L);

        sampleLink = new NetworkLink(srcIf, dstIf, LinkStatus.UP, LinkType.ETHERNET, 1000L, "Trunk Link");
        sampleLink.setId(100L);
        sampleLink.setCreatedAt(Instant.now());
        sampleLink.setUpdatedAt(Instant.now());

        sampleRequest = new LinkRequestDto(10L, 20L, LinkStatus.UP, LinkType.ETHERNET, 1000L, "Trunk Link");
    }

    @Test
    @DisplayName("Should successfully create valid network link")
    void testCreateLinkSuccess() {
        when(interfaceRepository.findById(10L)).thenReturn(Optional.of(srcIf));
        when(interfaceRepository.findById(20L)).thenReturn(Optional.of(dstIf));
        when(linkRepository.existsLinkBetweenInterfaces(10L, 20L)).thenReturn(false);
        when(linkRepository.save(any(NetworkLink.class))).thenReturn(sampleLink);

        LinkResponseDto result = linkService.createLink(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getSourceInterfaceName()).isEqualTo("Gi0/0");
        assertThat(result.getDestinationInterfaceName()).isEqualTo("Gi0/1");
        verify(linkRepository, times(1)).save(any(NetworkLink.class));
    }

    @Test
    @DisplayName("Should reject self-link on the same interface")
    void testCreateSelfLinkRejected() {
        sampleRequest.setDestinationInterfaceId(10L);

        assertThatThrownBy(() -> linkService.createLink(sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create self-link");

        verify(linkRepository, never()).save(any(NetworkLink.class));
    }

    @Test
    @DisplayName("Should reject link between two interfaces on the same device")
    void testCreateLinkSameDeviceRejected() {
        NetworkInterface sameDeviceIf = new NetworkInterface(srcIf.getDevice(), "Gi0/1", InterfaceType.GIGABIT_ETHERNET, null, null, null, AdminStatus.UP, OperationalStatus.UP, 1000L, null);
        sameDeviceIf.setId(30L);

        when(interfaceRepository.findById(10L)).thenReturn(Optional.of(srcIf));
        when(interfaceRepository.findById(30L)).thenReturn(Optional.of(sameDeviceIf));
        sampleRequest.setDestinationInterfaceId(30L);

        assertThatThrownBy(() -> linkService.createLink(sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create link between interfaces on the same device");

        verify(linkRepository, never()).save(any(NetworkLink.class));
    }

    @Test
    @DisplayName("Should reject duplicate link between same interface pair")
    void testCreateDuplicateLinkRejected() {
        when(interfaceRepository.findById(10L)).thenReturn(Optional.of(srcIf));
        when(interfaceRepository.findById(20L)).thenReturn(Optional.of(dstIf));
        when(linkRepository.existsLinkBetweenInterfaces(10L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> linkService.createLink(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with interfaces");

        verify(linkRepository, never()).save(any(NetworkLink.class));
    }
}
