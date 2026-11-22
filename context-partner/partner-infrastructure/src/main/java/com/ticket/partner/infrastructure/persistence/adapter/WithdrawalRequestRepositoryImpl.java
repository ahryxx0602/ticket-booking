package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.WithdrawalRequest;
import com.ticket.partner.domain.model.WithdrawalStatus;
import com.ticket.partner.domain.port.WithdrawalRequestRepository;
import com.ticket.partner.infrastructure.persistence.entity.WithdrawalRequestJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WithdrawalRequestRepositoryImpl implements WithdrawalRequestRepository {

    private final SpringDataWithdrawalRequestRepository repository;

    @Override
    public Optional<WithdrawalRequest> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<WithdrawalRequest> findByOperatorId(Long operatorId) {
        return repository.findByOperatorIdOrderByCreatedAtDesc(operatorId).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WithdrawalRequest> findByStatus(WithdrawalStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public WithdrawalRequest save(WithdrawalRequest request) {
        WithdrawalRequestJpaEntity entity = PartnerEntityMapper.toEntity(request);
        WithdrawalRequestJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }
}
