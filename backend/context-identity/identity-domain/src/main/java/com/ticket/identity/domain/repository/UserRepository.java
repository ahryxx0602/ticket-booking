package com.ticket.identity.domain.repository;

import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserStatus;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User save(User user);
    void deleteById(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> searchUsers(String keyword, String role, UserStatus status, int page, int size);
    long countUsers(String keyword, String role, UserStatus status);
    long countByRoleCode(String roleCode);
}
