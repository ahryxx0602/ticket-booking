package com.ticket.adapter;

import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.payment.domain.port.PaymentIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentIdentityPortAdapter implements PaymentIdentityPort {

    private final UserRepository userRepository;

    @Override
    public Optional<Long> findActiveUserIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username)
                .filter(user -> !user.isLocked())
                .map(User::getId);
    }
}
