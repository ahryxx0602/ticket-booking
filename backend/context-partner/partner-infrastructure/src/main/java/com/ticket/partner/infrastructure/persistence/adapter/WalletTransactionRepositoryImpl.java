package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.WalletTransaction;
import com.ticket.partner.domain.port.WalletTransactionRepository;
import com.ticket.partner.infrastructure.persistence.entity.WalletTransactionJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {

    private final SpringDataWalletTransactionRepository repository;

    @Override
    public Optional<WalletTransaction> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<WalletTransaction> findByOperatorId(Long operatorId) {
        return repository.findByOperatorIdOrderByCreatedAtDesc(operatorId).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletTransaction> findByWalletId(Long walletId) {
        return repository.findByWalletIdOrderByCreatedAtDesc(walletId).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBookingIdAndTransactionType(Long bookingId, com.ticket.partner.domain.model.WalletTransactionType transactionType) {
        if (bookingId == null || transactionType == null) {
            return false;
        }
        return repository.existsByBookingIdAndTransactionType(bookingId, transactionType);
    }

    @Override
    public WalletTransaction save(WalletTransaction transaction) {
        WalletTransactionJpaEntity entity = PartnerEntityMapper.toEntity(transaction);
        WalletTransactionJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }
}
