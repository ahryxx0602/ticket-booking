package com.ticket.booking.domain.port;

import java.util.Optional;

public interface IdentityUserPort {

    Optional<AuthenticatedUserInfo> findByUsername(String username);

    record AuthenticatedUserInfo(Long userId, boolean locked) {}
}
