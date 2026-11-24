package com.ticket.identity.domain.repository;

import com.ticket.identity.domain.model.SystemRole;

import java.util.List;
import java.util.Optional;

public interface SystemRoleRepository {
    Optional<SystemRole> findById(Long id);
    Optional<SystemRole> findByCode(String code);
    boolean existsByCode(String code);
    List<SystemRole> findAll();
    SystemRole save(SystemRole role);
    void deleteById(Long id);
}
