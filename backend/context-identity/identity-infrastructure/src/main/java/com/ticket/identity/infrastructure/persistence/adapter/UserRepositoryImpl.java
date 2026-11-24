package com.ticket.identity.infrastructure.persistence.adapter;

import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository jpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> searchUsers(String keyword, String role, UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.searchUsers(keyword, role, status, pageable)
                .map(this::toDomain)
                .getContent();
    }

    @Override
    public long countUsers(String keyword, String role, UserStatus status) {
        Pageable pageable = PageRequest.of(0, 1);
        return jpaRepository.searchUsers(keyword, role, status, pageable).getTotalElements();
    }

    @Override
    public long countByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return 0;
        }
        return jpaRepository.countByRoleCode(roleCode.trim().toUpperCase());
    }

    private User toDomain(UserJpaEntity entity) {
        UserRole baseRole = UserRole.fromString(entity.getRole());
        String customCode = (entity.getCustomRoleCode() != null && !entity.getCustomRoleCode().isBlank())
                ? entity.getCustomRoleCode()
                : baseRole.name();
        User u = User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .role(baseRole)
                .customRoleCode(customCode)
                .status(entity.getStatus())
                .build();
        u.setCreatedAt(entity.getCreatedAt());
        u.setUpdatedAt(entity.getUpdatedAt());
        return u;
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setPhone(user.getPhone());
        String baseRoleName = user.getRole() != null ? user.getRole().name() : "ROLE_CUSTOMER";
        entity.setRole(baseRoleName);
        entity.setCustomRoleCode(user.getEffectiveRoleCode());
        entity.setStatus(user.getStatus());
        return entity;
    }
}
