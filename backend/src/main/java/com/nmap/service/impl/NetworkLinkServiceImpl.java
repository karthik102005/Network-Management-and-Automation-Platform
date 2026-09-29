package com.nmap.service.impl;

import com.nmap.dto.LinkRequestDto;
import com.nmap.dto.LinkResponseDto;
import com.nmap.entity.LinkStatus;
import com.nmap.entity.LinkType;
import com.nmap.entity.NetworkInterface;
import com.nmap.entity.NetworkLink;
import com.nmap.exception.DuplicateResourceException;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkInterfaceRepository;
import com.nmap.repository.NetworkLinkRepository;
import com.nmap.service.NetworkLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class NetworkLinkServiceImpl implements NetworkLinkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkLinkServiceImpl.class);

    private final NetworkLinkRepository linkRepository;
    private final NetworkInterfaceRepository interfaceRepository;

    public NetworkLinkServiceImpl(NetworkLinkRepository linkRepository,
                                  NetworkInterfaceRepository interfaceRepository) {
        this.linkRepository = linkRepository;
        this.interfaceRepository = interfaceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkResponseDto> getAllLinks() {
        return linkRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LinkResponseDto getLinkById(Long id) {
        NetworkLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkLink", "id", id));
        return mapToResponseDto(link);
    }

    @Override
    public LinkResponseDto createLink(LinkRequestDto requestDto) {
        Long srcId = requestDto.getSourceInterfaceId();
        Long dstId = requestDto.getDestinationInterfaceId();

        log.info("Creating network link between interface {} and {}", srcId, dstId);

        if (Objects.equals(srcId, dstId)) {
            throw new IllegalArgumentException("Cannot create self-link: source and destination interface must be distinct");
        }

        NetworkInterface src = interfaceRepository.findById(srcId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkInterface", "id", srcId));
        NetworkInterface dst = interfaceRepository.findById(dstId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkInterface", "id", dstId));

        if (Objects.equals(src.getDevice().getId(), dst.getDevice().getId())) {
            throw new IllegalArgumentException("Cannot create link between interfaces on the same device");
        }

        if (linkRepository.existsLinkBetweenInterfaces(srcId, dstId)) {
            throw new DuplicateResourceException("NetworkLink", "interfaces", srcId + " <-> " + dstId);
        }

        NetworkLink link = new NetworkLink();
        link.setSourceInterface(src);
        link.setDestinationInterface(dst);
        link.setStatus(requestDto.getStatus() != null ? requestDto.getStatus() : LinkStatus.UP);
        link.setLinkType(requestDto.getLinkType() != null ? requestDto.getLinkType() : LinkType.ETHERNET);
        link.setBandwidthMbps(requestDto.getBandwidthMbps());
        link.setDescription(requestDto.getDescription());

        NetworkLink saved = linkRepository.save(link);
        log.info("Created link id={} ({} <-> {})", saved.getId(), src.getInterfaceName(), dst.getInterfaceName());
        return mapToResponseDto(saved);
    }

    @Override
    public LinkResponseDto updateLink(Long id, LinkRequestDto requestDto) {
        log.info("Updating link id={}", id);

        NetworkLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkLink", "id", id));

        Long srcId = requestDto.getSourceInterfaceId();
        Long dstId = requestDto.getDestinationInterfaceId();

        if (Objects.equals(srcId, dstId)) {
            throw new IllegalArgumentException("Cannot create self-link: source and destination interface must be distinct");
        }

        NetworkInterface src = interfaceRepository.findById(srcId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkInterface", "id", srcId));
        NetworkInterface dst = interfaceRepository.findById(dstId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkInterface", "id", dstId));

        if (Objects.equals(src.getDevice().getId(), dst.getDevice().getId())) {
            throw new IllegalArgumentException("Cannot create link between interfaces on the same device");
        }

        if (linkRepository.existsLinkBetweenInterfacesAndIdNot(srcId, dstId, id)) {
            throw new DuplicateResourceException("NetworkLink", "interfaces", srcId + " <-> " + dstId);
        }

        link.setSourceInterface(src);
        link.setDestinationInterface(dst);
        if (requestDto.getStatus() != null) {
            link.setStatus(requestDto.getStatus());
        }
        if (requestDto.getLinkType() != null) {
            link.setLinkType(requestDto.getLinkType());
        }
        link.setBandwidthMbps(requestDto.getBandwidthMbps());
        link.setDescription(requestDto.getDescription());

        NetworkLink updated = linkRepository.save(link);
        log.info("Updated link id={}", updated.getId());
        return mapToResponseDto(updated);
    }

    @Override
    public void deleteLink(Long id) {
        log.info("Deleting link id={}", id);
        if (!linkRepository.existsById(id)) {
            throw new ResourceNotFoundException("NetworkLink", "id", id);
        }
        linkRepository.deleteById(id);
        log.info("Deleted link id={}", id);
    }

    private LinkResponseDto mapToResponseDto(NetworkLink link) {
        NetworkInterface src = link.getSourceInterface();
        NetworkInterface dst = link.getDestinationInterface();

        return new LinkResponseDto(
                link.getId(),
                src != null ? src.getId() : null,
                src != null ? src.getInterfaceName() : null,
                src != null && src.getDevice() != null ? src.getDevice().getId() : null,
                src != null && src.getDevice() != null ? src.getDevice().getHostname() : null,
                dst != null ? dst.getId() : null,
                dst != null ? dst.getInterfaceName() : null,
                dst != null && dst.getDevice() != null ? dst.getDevice().getId() : null,
                dst != null && dst.getDevice() != null ? dst.getDevice().getHostname() : null,
                link.getStatus(),
                link.getLinkType(),
                link.getBandwidthMbps(),
                link.getDescription(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
