package com.nmap.service.impl;

import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.dto.DeviceRequestDto;
import com.nmap.dto.DeviceResponseDto;
import com.nmap.entity.DeviceStatus;
import com.nmap.entity.NetworkDevice;
import com.nmap.entity.NetworkInterface;
import com.nmap.entity.NetworkLink;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.network.DeviceReachabilityService;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.AlertService;
import com.nmap.service.DeviceConfigurationService;
import com.nmap.service.NetworkDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class NetworkDeviceServiceImpl implements NetworkDeviceService {

    private static final Logger log = LoggerFactory.getLogger(NetworkDeviceServiceImpl.class);

    private final NetworkDeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final NetworkLinkRepository linkRepository;
    private final DeviceReachabilityService reachabilityService;
    private final AlertService alertService;
    private final DeviceConfigurationService configurationService;

    public NetworkDeviceServiceImpl(NetworkDeviceRepository deviceRepository,
                                    NetworkInterfaceRepository interfaceRepository,
                                    NetworkLinkRepository linkRepository,
                                    DeviceReachabilityService reachabilityService,
                                    AlertService alertService,
                                    DeviceConfigurationService configurationService) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
        this.linkRepository = linkRepository;
        this.reachabilityService = reachabilityService;
        this.alertService = alertService;
        this.configurationService = configurationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponseDto> getAllDevices() {
        log.debug("Fetching all network devices");
        return deviceRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponseDto getDeviceById(Long id) {
        log.debug("Fetching network device with id: {}", id);
        NetworkDevice device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));
        return mapToResponseDto(device);
    }

    @Override
    public DeviceResponseDto createDevice(DeviceRequestDto requestDto) {
        log.info("Registering new network device: hostname={}, ip={}", requestDto.getHostname(), requestDto.getManagementIp());

        if (deviceRepository.existsByHostname(requestDto.getHostname())) {
            throw new DuplicateResourceException("NetworkDevice", "hostname", requestDto.getHostname());
        }

        if (deviceRepository.existsByManagementIp(requestDto.getManagementIp())) {
            throw new DuplicateResourceException("NetworkDevice", "managementIp", requestDto.getManagementIp());
        }

        NetworkDevice device = new NetworkDevice();
        device.setHostname(requestDto.getHostname().trim());
        device.setManagementIp(requestDto.getManagementIp().trim());
        device.setDeviceType(requestDto.getDeviceType());
        device.setVendor(requestDto.getVendor());
        device.setStatus(requestDto.getStatus() != null ? requestDto.getStatus() : DeviceStatus.UNKNOWN);
        device.setDescription(requestDto.getDescription());
        device.setLastSeen(requestDto.getLastSeen() != null ? requestDto.getLastSeen() : Instant.now());

        NetworkDevice savedDevice = deviceRepository.save(device);
        log.info("Successfully registered network device id={} ({})", savedDevice.getId(), savedDevice.getHostname());
        return mapToResponseDto(savedDevice);
    }

    @Override
    public DeviceResponseDto updateDevice(Long id, DeviceRequestDto requestDto) {
        log.info("Updating network device id={}: hostname={}, ip={}", id, requestDto.getHostname(), requestDto.getManagementIp());

        NetworkDevice device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));

        if (deviceRepository.existsByHostnameAndIdNot(requestDto.getHostname(), id)) {
            throw new DuplicateResourceException("NetworkDevice", "hostname", requestDto.getHostname());
        }

        if (deviceRepository.existsByManagementIpAndIdNot(requestDto.getManagementIp(), id)) {
            throw new DuplicateResourceException("NetworkDevice", "managementIp", requestDto.getManagementIp());
        }

        device.setHostname(requestDto.getHostname().trim());
        device.setManagementIp(requestDto.getManagementIp().trim());
        device.setDeviceType(requestDto.getDeviceType());
        device.setVendor(requestDto.getVendor());
        if (requestDto.getStatus() != null) {
            device.setStatus(requestDto.getStatus());
        }
        device.setDescription(requestDto.getDescription());
        if (requestDto.getLastSeen() != null) {
            device.setLastSeen(requestDto.getLastSeen());
        }

        NetworkDevice updatedDevice = deviceRepository.save(device);
        log.info("Successfully updated network device id={} ({})", updatedDevice.getId(), updatedDevice.getHostname());
        return mapToResponseDto(updatedDevice);
    }

    @Override
    public void deleteDevice(Long id) {
        log.info("Deleting network device id={}", id);
        if (!deviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("NetworkDevice", "id", id);
        }

        // Clean up interfaces and their attached links before deleting the device
        List<NetworkInterface> deviceInterfaces = interfaceRepository.findByDeviceId(id);
        if (!deviceInterfaces.isEmpty()) {
            Set<NetworkLink> linksToDelete = new HashSet<>();
            for (NetworkInterface iface : deviceInterfaces) {
                linksToDelete.addAll(linkRepository.findByInterfaceId(iface.getId()));
            }
            if (!linksToDelete.isEmpty()) {
                log.info("Removing {} links attached to interfaces of device id={}", linksToDelete.size(), id);
                linkRepository.deleteAll(linksToDelete);
            }
            log.info("Removing {} interfaces belonging to device id={}", deviceInterfaces.size(), id);
            interfaceRepository.deleteAll(deviceInterfaces);
        }

        // Clean up associated alerts before deleting the device
        alertService.deleteAlertsByDeviceId(id);

        // Clean up associated configurations before deleting the device
        configurationService.deleteConfigurationsByDeviceId(id);

        deviceRepository.deleteById(id);
        log.info("Successfully deleted network device id={}", id);
    }

    @Override
    public DeviceReachabilityResponseDto checkDeviceReachability(Long id, Integer timeoutMs) {
        log.info("Checking reachability for device id={}", id);
        NetworkDevice device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", id));

        // Perform probe on demand without mutating device records in database (Constraint 3)
        DeviceReachabilityResponseDto result = reachabilityService.checkReachability(
                device.getId(),
                device.getHostname(),
                device.getManagementIp(),
                timeoutMs
        );

        // Process fault and alert management lifecycle (Rules 4, 5, 6)
        alertService.processReachabilityResult(device, result);

        return result;
    }

    private DeviceResponseDto mapToResponseDto(NetworkDevice device) {
        return new DeviceResponseDto(
                device.getId(),
                device.getHostname(),
                device.getManagementIp(),
                device.getDeviceType(),
                device.getVendor(),
                device.getStatus(),
                device.getDescription(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                device.getLastSeen()
        );
    }
}
