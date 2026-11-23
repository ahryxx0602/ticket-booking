package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.OperatorWallet;
import com.ticket.partner.domain.port.OperatorWalletRepository;
import com.ticket.partner.infrastructure.persistence.entity.OperatorWalletJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataOperatorWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OperatorWalletRepositoryImpl implements OperatorWalletRepository {

    private final SpringDataOperatorWalletRepository repository;

    @Override
    public Optional<OperatorWallet> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public Optional<OperatorWallet> findByOperatorId(Long operatorId) {
        return repository.findByOperatorId(operatorId).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public OperatorWallet save(OperatorWallet wallet) {
        OperatorWalletJpaEntity entity = PartnerEntityMapper.toEntity(wallet);
        OperatorWalletJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }

    @Override
    public void creditPendingAtomic(Long operatorId, BigDecimal amount) {
        repository.creditPendingAtomic(operatorId, amount);
    }
}
