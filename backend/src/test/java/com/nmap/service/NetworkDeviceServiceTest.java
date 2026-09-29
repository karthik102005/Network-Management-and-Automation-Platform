package com.nmap.service;

import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.dto.DeviceRequestDto;
import com.nmap.dto.DeviceResponseDto;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.NetworkDevice;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.network.DeviceReachabilityService;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.impl.NetworkDeviceServiceImpl;
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
class NetworkDeviceServiceTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private NetworkInterfaceRepository interfaceRepository;

    @Mock
    private NetworkLinkRepository linkRepository;

    @Mock
    private DeviceReachabilityService reachabilityService;

    @Mock
    private AlertService alertService;

    @Mock
    private DeviceConfigurationService configurationService;

    @InjectMocks
    private NetworkDeviceServiceImpl deviceService;

    private NetworkDevice sampleDevice;
    private DeviceRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleDevice = new NetworkDevice("core-sw01", "192.168.1.1", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.UP, "Core Backbone Switch");
        sampleDevice.setId(1L);
        sampleDevice.setCreatedAt(Instant.now());
        sampleDevice.setUpdatedAt(Instant.now());
        sampleDevice.setLastSeen(Instant.now());

        sampleRequest = new DeviceRequestDto("core-sw01", "192.168.1.1", DeviceType.SWITCH, DeviceVendor.CISCO, DeviceStatus.UP, "Core Backbone Switch");
    }

    @Test
    @DisplayName("Should return all devices mapped to DTOs")
    void testGetAllDevices() {
        when(deviceRepository.findAll()).thenReturn(List.of(sampleDevice));

        List<DeviceResponseDto> result = deviceService.getAllDevices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHostname()).isEqualTo("core-sw01");
        assertThat(result.get(0).getManagementIp()).isEqualTo("192.168.1.1");
        verify(deviceRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return device by ID when found")
    void testGetDeviceByIdFound() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));

        DeviceResponseDto result = deviceService.getDeviceById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getHostname()).isEqualTo("core-sw01");
        verify(deviceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when device ID not found")
    void testGetDeviceByIdNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NetworkDevice not found with id: '99'");

        verify(deviceRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Should create device successfully when hostname and IP are unique")
    void testCreateDeviceSuccess() {
        when(deviceRepository.existsByHostname("core-sw01")).thenReturn(false);
        when(deviceRepository.existsByManagementIp("192.168.1.1")).thenReturn(false);
        when(deviceRepository.save(any(NetworkDevice.class))).thenAnswer(invocation -> {
            NetworkDevice saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        DeviceResponseDto result = deviceService.createDevice(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getHostname()).isEqualTo("core-sw01");
        verify(deviceRepository, times(1)).save(any(NetworkDevice.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate hostname")
    void testCreateDeviceDuplicateHostname() {
        when(deviceRepository.existsByHostname("core-sw01")).thenReturn(true);

        assertThatThrownBy(() -> deviceService.createDevice(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("hostname");

        verify(deviceRepository, never()).save(any(NetworkDevice.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate management IP")
    void testCreateDeviceDuplicateIp() {
        when(deviceRepository.existsByHostname("core-sw01")).thenReturn(false);
        when(deviceRepository.existsByManagementIp("192.168.1.1")).thenReturn(true);

        assertThatThrownBy(() -> deviceService.createDevice(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("managementIp");

        verify(deviceRepository, never()).save(any(NetworkDevice.class));
    }

    @Test
    @DisplayName("Should update device successfully")
    void testUpdateDeviceSuccess() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));
        when(deviceRepository.existsByHostnameAndIdNot("core-sw01", 1L)).thenReturn(false);
        when(deviceRepository.existsByManagementIpAndIdNot("192.168.1.1", 1L)).thenReturn(false);
        when(deviceRepository.save(any(NetworkDevice.class))).thenReturn(sampleDevice);

        DeviceResponseDto result = deviceService.updateDevice(1L, sampleRequest);

        assertThat(result).isNotNull();
        verify(deviceRepository, times(1)).save(sampleDevice);
    }

    @Test
    @DisplayName("Should delete device cleanly cascading to associated links, interfaces, and alerts")
    void testDeleteDeviceCascadesCleanly() {
        when(deviceRepository.existsById(1L)).thenReturn(true);

        com.nmap.entity.NetworkInterface iface1 = new com.nmap.entity.NetworkInterface();
        iface1.setId(10L);
        com.nmap.entity.NetworkInterface iface2 = new com.nmap.entity.NetworkInterface();
        iface2.setId(20L);

        when(interfaceRepository.findByDeviceId(1L)).thenReturn(List.of(iface1, iface2));

        com.nmap.entity.NetworkLink link1 = new com.nmap.entity.NetworkLink();
        link1.setId(100L);
        com.nmap.entity.NetworkLink link2 = new com.nmap.entity.NetworkLink();
        link2.setId(200L);

        when(linkRepository.findByInterfaceId(10L)).thenReturn(List.of(link1));
        when(linkRepository.findByInterfaceId(20L)).thenReturn(List.of(link2));

        deviceService.deleteDevice(1L);

        verify(linkRepository, times(1)).deleteAll(argThat((java.util.Collection<com.nmap.entity.NetworkLink> links) ->
                links.contains(link1) && links.contains(link2) && links.size() == 2));
        verify(interfaceRepository, times(1)).deleteAll(List.of(iface1, iface2));
        verify(alertService, times(1)).deleteAlertsByDeviceId(1L);
        verify(configurationService, times(1)).deleteConfigurationsByDeviceId(1L);
        verify(deviceRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent device")
    void testDeleteDeviceNotFound() {
        when(deviceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> deviceService.deleteDevice(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(deviceRepository, never()).deleteById(99L);
        verify(interfaceRepository, never()).deleteAll(any());
        verify(linkRepository, never()).deleteAll(any());
        verify(alertService, never()).deleteAlertsByDeviceId(any());
    }

    @Test
    @DisplayName("Should check reachability, trigger alert processing, and return result without mutating device entity")
    void testCheckDeviceReachabilitySuccess() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(sampleDevice));

        DeviceReachabilityResponseDto expectedDto = new DeviceReachabilityResponseDto(
                1L, "core-sw01", "192.168.1.1", true, "REACHABLE", "ICMP_ECHO", null, 15L, Instant.now(), "Responded"
        );
        when(reachabilityService.checkReachability(1L, "core-sw01", "192.168.1.1", 2000))
                .thenReturn(expectedDto);

        DeviceReachabilityResponseDto result = deviceService.checkDeviceReachability(1L, 2000);

        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo(1L);
        assertThat(result.isReachable()).isTrue();
        assertThat(result.getProbeMethod()).isEqualTo("ICMP_ECHO");

        // CRITICAL: Verify device was NOT mutated or saved to DB (Constraint 3)
        verify(deviceRepository, never()).save(any(NetworkDevice.class));
        verify(reachabilityService, times(1)).checkReachability(1L, "core-sw01", "192.168.1.1", 2000);
        // Verify alert processing was triggered
        verify(alertService, times(1)).processReachabilityResult(sampleDevice, expectedDto);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when checking reachability of non-existent device")
    void testCheckDeviceReachabilityNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.checkDeviceReachability(99L, 2000))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reachabilityService, never()).checkReachability(any(), any(), any(), any());
        verify(alertService, never()).processReachabilityResult(any(), any());
    }
}
