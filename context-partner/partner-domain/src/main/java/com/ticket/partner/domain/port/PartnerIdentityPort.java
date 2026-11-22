package com.ticket.partner.domain.port;

import java.util.Optional;

public interface PartnerIdentityPort {
    Optional<Long> findUserIdByUsername(String username);
    boolean existsUserById(Long userId);
    void grantOperatorRole(Long userId);
}
