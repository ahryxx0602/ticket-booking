package com.ticket.booking.domain.port;

import java.util.Optional;

public interface IdentityUserPort {

    Optional<AuthenticatedUserInfo> findByUsername(String username);

    record AuthenticatedUserInfo(Long userId, String phone, boolean locked) {
        public AuthenticatedUserInfo(Long userId, boolean locked) {
            this(userId, null, locked);
        }
    }
}
