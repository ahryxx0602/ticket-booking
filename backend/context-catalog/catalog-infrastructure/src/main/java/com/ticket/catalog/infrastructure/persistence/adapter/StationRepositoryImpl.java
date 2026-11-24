package com.ticket.catalog.infrastructure.persistence.adapter;

import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.repository.StationRepository;
import com.ticket.catalog.infrastructure.persistence.entity.StationJpaEntity;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StationRepositoryImpl implements StationRepository {

    private final SpringDataStationRepository jpaRepository;

    @Override
    public Optional<Station> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Station> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public List<Station> findAll() {
        return jpaRepository.findAllByOrderByCityAscCodeAsc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Station save(Station station) {
        StationJpaEntity entity = new StationJpaEntity();
        entity.setId(station.getId());
        entity.setCode(station.getCode());
        entity.setName(station.getName());
        entity.setCity(station.getCity());
        StationJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    private Station toDomain(StationJpaEntity entity) {
        return Station.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .city(entity.getCity())
                .build();
    }
}
