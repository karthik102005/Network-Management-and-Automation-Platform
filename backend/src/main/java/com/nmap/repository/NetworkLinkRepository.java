package com.nmap.repository;

import com.nmap.entity.LinkStatus;
import com.nmap.entity.NetworkLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkLinkRepository extends JpaRepository<NetworkLink, Long> {

    List<NetworkLink> findByStatus(LinkStatus status);

    @Query("SELECT l FROM NetworkLink l WHERE l.sourceInterface.id = :ifId OR l.destinationInterface.id = :ifId")
    List<NetworkLink> findByInterfaceId(@Param("ifId") Long ifId);

    @Query("SELECT COUNT(l) > 0 FROM NetworkLink l WHERE " +
           "(l.sourceInterface.id = :srcId AND l.destinationInterface.id = :dstId) OR " +
           "(l.sourceInterface.id = :dstId AND l.destinationInterface.id = :srcId)")
    boolean existsLinkBetweenInterfaces(@Param("srcId") Long srcId, @Param("dstId") Long dstId);

    @Query("SELECT COUNT(l) > 0 FROM NetworkLink l WHERE l.id <> :id AND (" +
           "(l.sourceInterface.id = :srcId AND l.destinationInterface.id = :dstId) OR " +
           "(l.sourceInterface.id = :dstId AND l.destinationInterface.id = :srcId))")
    boolean existsLinkBetweenInterfacesAndIdNot(@Param("srcId") Long srcId, @Param("dstId") Long dstId, @Param("id") Long id);
}
