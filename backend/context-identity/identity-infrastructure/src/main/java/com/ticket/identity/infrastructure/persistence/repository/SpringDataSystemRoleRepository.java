package com.ticket.identity.infrastructure.persistence.repository;

import com.ticket.identity.infrastructure.persistence.entity.SystemRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSystemRoleRepository extends JpaRepository<SystemRoleJpaEntity, Long> {
    Optional<SystemRoleJpaEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<SystemRoleJpaEntity> findAllByOrderBySystemDefaultDescIdAsc();
}
