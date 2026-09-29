package com.nmap.repository;

import com.nmap.entity.NetworkInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkInterfaceRepository extends JpaRepository<NetworkInterface, Long> {

    List<NetworkInterface> findByDeviceId(Long deviceId);

    boolean existsByDeviceIdAndInterfaceName(Long deviceId, String interfaceName);

    boolean existsByDeviceIdAndInterfaceNameAndIdNot(Long deviceId, String interfaceName, Long id);

    Optional<NetworkInterface> findByDeviceIdAndInterfaceName(Long deviceId, String interfaceName);

    long countByDeviceId(Long deviceId);
}
