package com.nmap.repository;

import com.nmap.entity.DeviceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceConfigurationRepository extends JpaRepository<DeviceConfiguration, Long> {

    List<DeviceConfiguration> findByDeviceIdOrderByVersionDesc(Long deviceId);

    Optional<DeviceConfiguration> findByDeviceIdAndVersion(Long deviceId, Integer version);

    Optional<DeviceConfiguration> findTopByDeviceIdOrderByVersionDesc(Long deviceId);

    List<DeviceConfiguration> findByDeviceIdAndActiveTrue(Long deviceId);

    @Modifying
    @Query("DELETE FROM DeviceConfiguration c WHERE c.device.id = :deviceId")
    void deleteByDeviceId(@Param("deviceId") Long deviceId);
}
