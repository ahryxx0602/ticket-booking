package com.ticket.payment.domain.port;

import java.util.Optional;

public interface PaymentIdentityPort {
    Optional<Long> findActiveUserIdByUsername(String username);
}
