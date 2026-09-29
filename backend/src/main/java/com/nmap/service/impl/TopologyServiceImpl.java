package com.nmap.service.impl;

import com.nmap.dto.TopologyResponseDto;
import com.nmap.dto.TopologyResponseDto.TopologyLinkDto;
import com.nmap.dto.TopologyResponseDto.TopologyNodeDto;
import com.nmap.entity.NetworkDevice;
import com.nmap.entity.NetworkInterface;
import com.nmap.entity.NetworkLink;
import com.nmap.repository.NetworkDeviceRepository;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TopologyServiceImpl implements TopologyService {

    private static final Logger log = LoggerFactory.getLogger(TopologyServiceImpl.class);

    private final NetworkDeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final NetworkLinkRepository linkRepository;

    public TopologyServiceImpl(NetworkDeviceRepository deviceRepository,
                               NetworkInterfaceRepository interfaceRepository,
                               NetworkLinkRepository linkRepository) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    public TopologyResponseDto getTopology() {
        log.debug("Generating network topology graph");

        List<NetworkDevice> devices = deviceRepository.findAll();
        List<NetworkLink> links = linkRepository.findAll();

        List<TopologyNodeDto> nodes = devices.stream()
                .map(device -> new TopologyNodeDto(
                        device.getId(),
                        device.getHostname(),
                        device.getManagementIp(),
                        device.getDeviceType(),
                        device.getVendor(),
                        device.getStatus(),
                        interfaceRepository.countByDeviceId(device.getId())
                ))
                .collect(Collectors.toList());

        List<TopologyLinkDto> linkDtos = links.stream()
                .map(link -> {
                    NetworkInterface src = link.getSourceInterface();
                    NetworkInterface dst = link.getDestinationInterface();
                    return new TopologyLinkDto(
                            link.getId(),
                            src != null && src.getDevice() != null ? src.getDevice().getId() : null,
                            src != null && src.getDevice() != null ? src.getDevice().getHostname() : null,
                            src != null ? src.getId() : null,
                            src != null ? src.getInterfaceName() : null,
                            dst != null && dst.getDevice() != null ? dst.getDevice().getId() : null,
                            dst != null && dst.getDevice() != null ? dst.getDevice().getHostname() : null,
                            dst != null ? dst.getId() : null,
                            dst != null ? dst.getInterfaceName() : null,
                            link.getStatus(),
                            link.getLinkType(),
                            link.getBandwidthMbps()
                    );
                })
                .collect(Collectors.toList());

        log.debug("Topology graph generated with {} nodes and {} links", nodes.size(), linkDtos.size());
        return new TopologyResponseDto(nodes, linkDtos);
    }
}
