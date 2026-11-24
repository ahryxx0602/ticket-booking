package com.ticket.identity.infrastructure.persistence.repository;

import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:role IS NULL OR :role = '' " +
           " OR (:role = 'CATEGORY_SYSTEM' AND u.role = 'ROLE_ADMIN') " +
           " OR (:role != 'CATEGORY_SYSTEM' AND (u.customRoleCode = :role OR (u.customRoleCode IS NULL AND u.role = :role)))) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<UserJpaEntity> searchUsers(@Param("keyword") String keyword,
                                    @Param("role") String role,
                                    @Param("status") UserStatus status,
                                    Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.customRoleCode = :roleCode OR (u.customRoleCode IS NULL AND u.role = :roleCode)")
    long countByRoleCode(@Param("roleCode") String roleCode);
}
