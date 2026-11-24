package com.ticket.identity.infrastructure.persistence.adapter;

import com.ticket.identity.domain.model.AdminPermissionCatalog;
import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.infrastructure.persistence.entity.SystemRoleJpaEntity;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataSystemRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SystemRoleRepositoryImpl implements SystemRoleRepository {

    private final SpringDataSystemRoleRepository jpaRepository;

    @Override
    public Optional<SystemRole> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SystemRole> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return jpaRepository.findByCode(code.trim().toUpperCase()).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null) {
            return false;
        }
        return jpaRepository.existsByCode(code.trim().toUpperCase());
    }

    @Override
    public List<SystemRole> findAll() {
        return jpaRepository.findAllByOrderBySystemDefaultDescIdAsc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public SystemRole save(SystemRole role) {
        SystemRoleJpaEntity entity = toEntity(role);
        SystemRoleJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private SystemRole toDomain(SystemRoleJpaEntity entity) {
        List<String> perms = "ROLE_ADMIN".equalsIgnoreCase(entity.getCode())
                ? AdminPermissionCatalog.allPermissionsList()
                : AdminPermissionCatalog.parsePermissionsText(entity.getPermissionsText());
        SystemRole role = SystemRole.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .permissions(perms)
                .systemDefault(entity.isSystemDefault())
                .build();
        role.setCreatedAt(entity.getCreatedAt());
        role.setUpdatedAt(entity.getUpdatedAt());
        return role;
    }

    private SystemRoleJpaEntity toEntity(SystemRole role) {
        SystemRoleJpaEntity entity = new SystemRoleJpaEntity();
        entity.setId(role.getId());
        entity.setCode(role.getCode());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setCategory(role.getCategory() != null ? role.getCategory() : "SYSTEM");
        entity.setPermissionsText(AdminPermissionCatalog.joinPermissionsText(role.getPermissions()));
        entity.setSystemDefault(role.isSystemDefault());
        return entity;
    }
}
