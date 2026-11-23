package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.infrastructure.persistence.entity.SystemSettingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataSystemSettingRepository extends JpaRepository<SystemSettingJpaEntity, Long> {
    Optional<SystemSettingJpaEntity> findBySettingKey(String settingKey);
}
