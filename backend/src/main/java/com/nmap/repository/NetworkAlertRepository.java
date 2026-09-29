package com.nmap.repository;

import com.nmap.entity.AlertSeverity;
import com.nmap.entity.AlertStatus;
import com.nmap.entity.AlertType;
import com.nmap.entity.NetworkAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkAlertRepository extends JpaRepository<NetworkAlert, Long> {

    List<NetworkAlert> findAllByOrderByCreatedAtDesc();

    List<NetworkAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<NetworkAlert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity);

    List<NetworkAlert> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    List<NetworkAlert> findByDeviceIdAndStatusIn(Long deviceId, Collection<AlertStatus> statuses);

    Optional<NetworkAlert> findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
            Long deviceId, AlertType alertType, Collection<AlertStatus> statuses);

    List<NetworkAlert> findByDeviceId(Long deviceId);

    void deleteByDeviceId(Long deviceId);
}
