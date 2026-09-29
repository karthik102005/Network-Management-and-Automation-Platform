package com.nmap.service;

import com.nmap.dto.InterfaceRequestDto;
import com.nmap.dto.InterfaceResponseDto;
import com.nmap.entity.*;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.impl.NetworkInterfaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkInterfaceServiceTest {

    @Mock
    private NetworkInterfaceRepository interfaceRepository;

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private NetworkLinkRepository linkRepository;

    @InjectMocks
    private NetworkInterfaceServiceImpl interfaceService;

    private NetworkDevice sampleDevice;
    private NetworkInterface sampleInterface;
    private InterfaceRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleDevice = new NetworkDevice("core-r1", "10.0.0.1", DeviceType.ROUTER, DeviceVendor.CISCO, DeviceStatus.UP, "Core Router");
        sampleDevice.setId(1L);

        sampleInterface = new NetworkInterface(sampleDevice, "GigabitEthernet0/0/0", InterfaceType.GIGABIT_ETHERNET,
                "10.0.0.1", 24, "00:1A:2B:3C:4D:5E", AdminStatus.UP, OperationalStatus.UP, 1000L, "WAN Link");
        sampleInterface.setId(10L);
        sampleInterface.setCreatedAt(Instant.now());
        sampleInterface.setUpdatedAt(Instant.now());

        sampleRequest = new InterfaceRequestDto(1L, "GigabitEthernet0/0/0", InterfaceType.GIGABIT_ETHERNET,
                "10.0.0.1", 24, "00:1A:2B:3C:4D:5E", AdminStatus.UP, OperationalStatus.UP, 1000L, "WAN Link");
    }

    @Test
    @DisplayName("Should successfully create interface for existing device")
    void testCreateInterfaceSuccess() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(interfaceRepository.existsByDeviceIdAndInterfaceName(1L, "GigabitEthernet0/0/0")).thenReturn(false);
        when(interfaceRepository.save(any(NetworkInterface.class))).thenReturn(sampleInterface);

        InterfaceResponseDto result = interfaceService.createInterface(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getInterfaceName()).isEqualTo("GigabitEthernet0/0/0");
        assertThat(result.getDeviceId()).isEqualTo(1L);
        verify(interfaceRepository, times(1)).save(any(NetworkInterface.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate interface name for same device")
    void testCreateInterfaceDuplicateName() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(interfaceRepository.existsByDeviceIdAndInterfaceName(1L, "GigabitEthernet0/0/0")).thenReturn(true);

        assertThatThrownBy(() -> interfaceService.createInterface(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with interfaceName: 'GigabitEthernet0/0/0'");

        verify(interfaceRepository, never()).save(any(NetworkInterface.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating interface for non-existent device")
    void testCreateInterfaceDeviceNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());
        sampleRequest.setDeviceId(99L);

        assertThatThrownBy(() -> interfaceService.createInterface(sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkDevice not found with id: '99'");
    }

    @Test
    @DisplayName("Should delete interface and clean up attached links")
    void testDeleteInterfaceSuccess() {
        when(interfaceRepository.existsById(10L)).thenReturn(true);
        when(linkRepository.findByInterfaceId(10L)).thenReturn(List.of());
        doNothing().when(interfaceRepository).deleteById(10L);

        interfaceService.deleteInterface(10L);

        verify(interfaceRepository, times(1)).deleteById(10L);
    }
}
