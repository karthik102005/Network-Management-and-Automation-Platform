package com.nmap.repository;

import com.nmap.entity.AutomationChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationChangeRequestRepository extends JpaRepository<AutomationChangeRequest, Long> {

    List<AutomationChangeRequest> findAllByOrderByCreatedAtDesc();

    List<AutomationChangeRequest> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
