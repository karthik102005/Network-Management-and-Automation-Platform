package com.nmap.service.impl;

import com.nmap.dto.AlertResponseDto;
import com.nmap.dto.DeviceReachabilityResponseDto;
import com.nmap.entity.AlertSeverity;
import com.nmap.entity.AlertStatus;
import com.nmap.entity.AlertType;
import com.nmap.entity.NetworkAlert;
import com.nmap.entity.NetworkDevice;
import com.nmap.exception.ResourceNotFoundException;
import com.nmap.repository.NetworkAlertRepository;
import com.nmap.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    private final NetworkAlertRepository alertRepository;

    public AlertServiceImpl(NetworkAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDto> getAlerts(AlertStatus status, AlertSeverity severity, Long deviceId) {
        log.debug("Fetching alerts with filters: status={}, severity={}, deviceId={}", status, severity, deviceId);
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> severity == null || a.getSeverity() == severity)
                .filter(a -> deviceId == null || (a.getDevice() != null && a.getDevice().getId().equals(deviceId)))
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponseDto getAlertById(Long id) {
        log.debug("Fetching alert with id: {}", id);
        NetworkAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkAlert", "id", id));
        return mapToResponseDto(alert);
    }

    @Override
    public AlertResponseDto acknowledgeAlert(Long id) {
        log.info("Acknowledging alert id={}", id);
        NetworkAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkAlert", "id", id));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new IllegalArgumentException("Cannot acknowledge an already resolved alert (id=" + id + ").");
        }

        if (alert.getStatus() == AlertStatus.ACKNOWLEDGED) {
            return mapToResponseDto(alert);
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        NetworkAlert saved = alertRepository.save(alert);
        log.info("Successfully acknowledged alert id={}", saved.getId());
        return mapToResponseDto(saved);
    }

    @Override
    public AlertResponseDto resolveAlert(Long id) {
        log.info("Resolving alert id={}", id);
        NetworkAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkAlert", "id", id));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return mapToResponseDto(alert);
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        NetworkAlert saved = alertRepository.save(alert);
        log.info("Successfully resolved alert id={}", saved.getId());
        return mapToResponseDto(saved);
    }

    @Override
    public void processReachabilityResult(NetworkDevice device, DeviceReachabilityResponseDto result) {
        if (device == null || result == null) {
            return;
        }

        // Rule 4: Generate DEVICE_UNREACHABLE only when probe result explicitly indicates UNREACHABLE
        if (!result.isReachable() && "UNREACHABLE".equalsIgnoreCase(result.getReachabilityStatus())) {
            // Rule 5: Suppress duplicate active alerts for the same device and alert type
            Optional<NetworkAlert> existingActive = alertRepository.findFirstByDeviceIdAndAlertTypeAndStatusInOrderByCreatedAtDesc(
                    device.getId(),
                    AlertType.DEVICE_UNREACHABLE,
                    List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
            );

            if (existingActive.isPresent()) {
                log.info("Active alert already exists for device id={}, suppressing duplicate alert generation", device.getId());
                return;
            }

            NetworkAlert alert = new NetworkAlert(
                    device,
                    AlertType.DEVICE_UNREACHABLE,
                    AlertSeverity.CRITICAL,
                    AlertStatus.OPEN,
                    result.getDetails() != null ? result.getDetails() : "Device is unreachable via ICMP echo and management ports.",
                    "REACHABILITY_MONITOR"
            );
            alert.setCreatedAt(Instant.now());
            NetworkAlert saved = alertRepository.save(alert);
            log.info("Created new DEVICE_UNREACHABLE alert id={} for device id={}", saved != null ? saved.getId() : "new", device.getId());

        } else if (result.isReachable()) {
            // Rule 6: A successful reachability check must resolve any matching active DEVICE_UNREACHABLE alert
            List<NetworkAlert> activeAlerts = alertRepository.findByDeviceIdAndStatusIn(
                    device.getId(),
                    List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)
            );

            for (NetworkAlert alert : activeAlerts) {
                if (alert.getAlertType() == AlertType.DEVICE_UNREACHABLE) {
                    alert.setStatus(AlertStatus.RESOLVED);
                    alert.setResolvedAt(Instant.now());
                    String recoveryNote = String.format(" [Auto-recovered: Device confirmed reachable via %s (%sms)]",
                            result.getProbeMethod() != null ? result.getProbeMethod() : "probe",
                            result.getResponseTimeMs() != null ? result.getResponseTimeMs() : 0);
                    alert.setMessage(alert.getMessage() + recoveryNote);
                    alertRepository.save(alert);
                    log.info("Auto-resolved alert id={} for device id={} on reachability recovery", alert.getId(), device.getId());
                }
            }
        }
        // Note: INVALID_TARGET and INCONCLUSIVE results do not generate or resolve DEVICE_UNREACHABLE alerts
    }

    @Override
    public void deleteAlertsByDeviceId(Long deviceId) {
        List<NetworkAlert> alerts = alertRepository.findByDeviceId(deviceId);
        if (!alerts.isEmpty()) {
            log.info("Deleting {} alerts associated with device id={}", alerts.size(), deviceId);
            alertRepository.deleteAll(alerts);
        }
    }

    private AlertResponseDto mapToResponseDto(NetworkAlert alert) {
        Long deviceId = alert.getDevice() != null ? alert.getDevice().getId() : null;
        String deviceHostname = alert.getDevice() != null ? alert.getDevice().getHostname() : null;
        String deviceIp = alert.getDevice() != null ? alert.getDevice().getManagementIp() : null;

        return new AlertResponseDto(
                alert.getId(),
                deviceId,
                deviceHostname,
                deviceIp,
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                alert.getSource(),
                alert.getCreatedAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt()
        );
    }
}
