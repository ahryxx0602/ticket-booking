package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.Driver;
import com.ticket.partner.domain.port.DriverRepository;
import com.ticket.partner.infrastructure.persistence.entity.DriverJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DriverRepositoryImpl implements DriverRepository {

    private final SpringDataDriverRepository repository;

    @Override
    public Optional<Driver> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public Optional<Driver> findByIdAndOperatorId(Long id, Long operatorId) {
        return repository.findByIdAndOperatorId(id, operatorId).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<Driver> findByOperatorId(Long operatorId) {
        return repository.findByOperatorId(operatorId).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Driver save(Driver driver) {
        DriverJpaEntity entity = PartnerEntityMapper.toEntity(driver);
        DriverJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }
}
