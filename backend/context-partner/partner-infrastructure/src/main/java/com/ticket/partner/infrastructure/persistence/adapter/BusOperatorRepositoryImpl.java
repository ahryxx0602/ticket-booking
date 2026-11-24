package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.BusOperator;
import com.ticket.partner.domain.model.OperatorStatus;
import com.ticket.partner.domain.port.BusOperatorRepository;
import com.ticket.partner.infrastructure.persistence.entity.BusOperatorJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataBusOperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BusOperatorRepositoryImpl implements BusOperatorRepository {

    private final SpringDataBusOperatorRepository repository;

    @Override
    public Optional<BusOperator> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public Optional<BusOperator> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<BusOperator> findByStatus(OperatorStatus status) {
        return repository.findByStatus(status).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BusOperator> findAll() {
        return repository.findAll().stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTaxCode(String taxCode) {
        return repository.existsByTaxCode(taxCode);
    }

    @Override
    public BusOperator save(BusOperator operator) {
        BusOperatorJpaEntity entity = PartnerEntityMapper.toEntity(operator);
        BusOperatorJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
        repository.flush();
    }
}
