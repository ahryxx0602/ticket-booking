package com.ticket.notification.infrastructure.persistence.adapter;

import com.ticket.notification.domain.model.TelegramAccountLink;
import com.ticket.notification.domain.port.TelegramAccountLinkRepository;
import com.ticket.notification.infrastructure.persistence.entity.TelegramAccountLinkJpaEntity;
import com.ticket.notification.infrastructure.persistence.repository.SpringDataTelegramAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TelegramAccountLinkRepositoryImpl implements TelegramAccountLinkRepository {

    private final SpringDataTelegramAccountLinkRepository jpaRepository;

    @Override
    public TelegramAccountLink save(TelegramAccountLink domain) {
        TelegramAccountLinkJpaEntity entity = toEntity(domain);
        TelegramAccountLinkJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TelegramAccountLink> findByTelegramChatId(Long telegramChatId) {
        return jpaRepository.findByTelegramChatId(telegramChatId).map(this::toDomain);
    }

    @Override
    public Optional<TelegramAccountLink> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public List<TelegramAccountLink> findByRoleAndActiveTrue(String role) {
        return jpaRepository.findByRoleAndActiveTrue(role).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByTelegramChatId(Long telegramChatId) {
        jpaRepository.deleteByTelegramChatId(telegramChatId);
    }

    private TelegramAccountLink toDomain(TelegramAccountLinkJpaEntity entity) {
        TelegramAccountLink domain = TelegramAccountLink.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .telegramChatId(entity.getTelegramChatId())
                .role(entity.getRole())
                .active(entity.isActive())
                .linkedAt(entity.getLinkedAt())
                .build();
        domain.setCreatedAt(entity.getLinkedAt());
        return domain;
    }

    private TelegramAccountLinkJpaEntity toEntity(TelegramAccountLink domain) {
        TelegramAccountLinkJpaEntity entity = new TelegramAccountLinkJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTelegramChatId(domain.getTelegramChatId());
        entity.setRole(domain.getRole());
        entity.setActive(domain.isActive());
        entity.setLinkedAt(domain.getLinkedAt());
        return entity;
    }
}
