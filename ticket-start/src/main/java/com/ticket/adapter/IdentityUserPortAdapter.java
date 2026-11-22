package com.ticket.adapter;

import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdentityUserPortAdapter implements IdentityUserPort {

    private final UserRepository userRepository;

    @Override
    public Optional<AuthenticatedUserInfo> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> new AuthenticatedUserInfo(user.getId(), user.getPhone(), user.isLocked()));
    }
}
