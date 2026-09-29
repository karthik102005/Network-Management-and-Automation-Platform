package com.nmap.service;

import com.nmap.dto.LinkRequestDto;
import com.nmap.dto.LinkResponseDto;

import java.util.List;

public interface NetworkLinkService {

    List<LinkResponseDto> getAllLinks();

    LinkResponseDto getLinkById(Long id);

    LinkResponseDto createLink(LinkRequestDto requestDto);

    LinkResponseDto updateLink(Long id, LinkRequestDto requestDto);

    void deleteLink(Long id);
}
