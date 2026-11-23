package com.ticket.catalog.infrastructure.persistence.repository;

import com.ticket.catalog.infrastructure.persistence.entity.StationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataStationRepository extends JpaRepository<StationJpaEntity, Long> {
    Optional<StationJpaEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<StationJpaEntity> findAllByOrderByCityAscCodeAsc();
}
