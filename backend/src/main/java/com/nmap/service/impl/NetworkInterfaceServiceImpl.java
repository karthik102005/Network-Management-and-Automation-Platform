package com.nmap.service.impl;

import com.nmap.dto.InterfaceRequestDto;
import com.nmap.dto.InterfaceResponseDto;
import com.nmap.entity.AdminStatus;
import com.nmap.entity.InterfaceType;
import com.nmap.entity.NetworkDevice;
import com.nmap.entity.NetworkInterface;
import com.nmap.entity.OperationalStatus;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.NetworkInterfaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NetworkInterfaceServiceImpl implements NetworkInterfaceService {

    private static final Logger log = LoggerFactory.getLogger(NetworkInterfaceServiceImpl.class);

    private final NetworkInterfaceRepository interfaceRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final NetworkLinkRepository linkRepository;

    public NetworkInterfaceServiceImpl(NetworkInterfaceRepository interfaceRepository,
                                       NetworkDeviceRepository deviceRepository,
                                       NetworkLinkRepository linkRepository) {
        this.interfaceRepository = interfaceRepository;
        this.deviceRepository = deviceRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterfaceResponseDto> getAllInterfaces() {
        return interfaceRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InterfaceResponseDto getInterfaceById(Long id) {
        NetworkInterface networkInterface = interfaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkInterface", "id", id));
        return mapToResponseDto(networkInterface);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterfaceResponseDto> getInterfacesByDeviceId(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("NetworkDevice", "id", deviceId);
        }
        return interfaceRepository.findByDeviceId(deviceId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public InterfaceResponseDto createInterface(InterfaceRequestDto requestDto) {
        log.info("Creating interface '{}' on device id={}", requestDto.getInterfaceName(), requestDto.getDeviceId());

        NetworkDevice device = deviceRepository.findById(requestDto.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("NetworkDevice", "id", requestDto.getDeviceId()));

        String normalizedName = requestDto.getInterfaceName().trim();
        if (interfaceRepository.existsByDeviceIdAndInterfaceName(device.getId(), normalizedName)) {
            throw new DuplicateResourceException("NetworkInterface", "interfaceName", normalizedName);
        }

        NetworkInterface iface = new NetworkInterface();
        iface.setDevice(device);
        iface.setInterfaceName(normalizedName);
        iface.setInterfaceType(requestDto.getInterfaceType() != null ? requestDto.getInterfaceType() : InterfaceType.GIGABIT_ETHERNET);
        iface.setIpAddress(requestDto.getIpAddress() != null && !requestDto.getIpAddress().isBlank() ? requestDto.getIpAddress().trim() : null);
        iface.setSubnetPrefix(requestDto.getSubnetPrefix());
        iface.setMacAddress(requestDto.getMacAddress() != null && !requestDto.getMacAddress().isBlank() ? requestDto.getMacAddress().trim() : null);
        iface.setAdminStatus(requestDto.getAdminStatus() != null ? requestDto.getAdminStatus() : AdminStatus.UP);
        iface.setOperationalStatus(requestDto.getOperationalStatus() != null ? requestDto.getOperationalStatus() : OperationalStatus.UNKNOWN);
        iface.setSpeedMbps(requestDto.getSpeedMbps());
        iface.setDescription(requestDto.getDescription());

        NetworkInterface saved = interfaceRepository.save(iface);
        log.info("Created interface id={} ({}) on device id={}", saved.getId(), saved.getInterfaceName(), device.getId());
        return mapToResponseDto(saved);
    }

    @Override
    public InterfaceResponseDto updateInterface(Long id, InterfaceRequestDto requestDto) {
        log.info("Updating interface id={}", id);

        NetworkInterface iface = interfaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkInterface", "id", id));

        String normalizedName = requestDto.getInterfaceName().trim();
        if (interfaceRepository.existsByDeviceIdAndInterfaceNameAndIdNot(iface.getDevice().getId(), normalizedName, id)) {
            throw new DuplicateResourceException("NetworkInterface", "interfaceName", normalizedName);
        }

        iface.setInterfaceName(normalizedName);
        if (requestDto.getInterfaceType() != null) {
            iface.setInterfaceType(requestDto.getInterfaceType());
        }
        iface.setIpAddress(requestDto.getIpAddress() != null && !requestDto.getIpAddress().isBlank() ? requestDto.getIpAddress().trim() : null);
        iface.setSubnetPrefix(requestDto.getSubnetPrefix());
        iface.setMacAddress(requestDto.getMacAddress() != null && !requestDto.getMacAddress().isBlank() ? requestDto.getMacAddress().trim() : null);
        if (requestDto.getAdminStatus() != null) {
            iface.setAdminStatus(requestDto.getAdminStatus());
        }
        if (requestDto.getOperationalStatus() != null) {
            iface.setOperationalStatus(requestDto.getOperationalStatus());
        }
        iface.setSpeedMbps(requestDto.getSpeedMbps());
        iface.setDescription(requestDto.getDescription());

        NetworkInterface updated = interfaceRepository.save(iface);
        log.info("Updated interface id={}", updated.getId());
        return mapToResponseDto(updated);
    }

    @Override
    public void deleteInterface(Long id) {
        log.info("Deleting interface id={}", id);
        if (!interfaceRepository.existsById(id)) {
            throw new ResourceNotFoundException("NetworkInterface", "id", id);
        }

        // Clean up attached links to prevent foreign key integrity violations
        var attachedLinks = linkRepository.findByInterfaceId(id);
        if (!attachedLinks.isEmpty()) {
            log.info("Removing {} links attached to interface id={}", attachedLinks.size(), id);
            linkRepository.deleteAll(attachedLinks);
        }

        interfaceRepository.deleteById(id);
        log.info("Deleted interface id={}", id);
    }

    private InterfaceResponseDto mapToResponseDto(NetworkInterface iface) {
        return new InterfaceResponseDto(
                iface.getId(),
                iface.getDevice() != null ? iface.getDevice().getId() : null,
                iface.getDevice() != null ? iface.getDevice().getHostname() : null,
                iface.getInterfaceName(),
                iface.getInterfaceType(),
                iface.getIpAddress(),
                iface.getSubnetPrefix(),
                iface.getMacAddress(),
                iface.getAdminStatus(),
                iface.getOperationalStatus(),
                iface.getSpeedMbps(),
                iface.getDescription(),
                iface.getCreatedAt(),
                iface.getUpdatedAt()
        );
    }
}
