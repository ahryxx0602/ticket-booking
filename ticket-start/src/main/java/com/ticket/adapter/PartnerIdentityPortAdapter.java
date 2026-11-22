package com.ticket.adapter;

import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.partner.domain.port.PartnerIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PartnerIdentityPortAdapter implements PartnerIdentityPort {

    private final UserRepository userRepository;

    @Override
    public Optional<Long> findUserIdByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username).map(u -> u.getId());
    }

    @Override
    public boolean existsUserById(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId).isPresent();
    }

    @Override
    @Transactional
    public void grantOperatorRole(Long userId) {
        if (userId == null) return;
        userRepository.findById(userId).ifPresent(user -> {
            user.changeRole(UserRole.ROLE_OPERATOR);
            userRepository.save(user);
        });
    }
}
