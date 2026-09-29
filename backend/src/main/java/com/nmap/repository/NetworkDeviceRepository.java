package com.nmap.repository;

import com.nmap.entity.DeviceStatus;
import com.nmap.entity.DeviceType;
import com.nmap.entity.DeviceVendor;
import com.nmap.entity.NetworkDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {

    Optional<NetworkDevice> findByHostname(String hostname);

    Optional<NetworkDevice> findByManagementIp(String managementIp);

    boolean existsByHostname(String hostname);

    boolean existsByHostnameAndIdNot(String hostname, Long id);

    boolean existsByManagementIp(String managementIp);

    boolean existsByManagementIpAndIdNot(String managementIp, Long id);

    List<NetworkDevice> findByDeviceType(DeviceType deviceType);

    List<NetworkDevice> findByVendor(DeviceVendor vendor);

    List<NetworkDevice> findByStatus(DeviceStatus status);
}
