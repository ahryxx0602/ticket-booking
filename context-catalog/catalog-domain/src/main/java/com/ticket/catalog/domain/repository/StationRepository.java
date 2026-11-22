package com.ticket.catalog.domain.repository;

import com.ticket.catalog.domain.model.Station;

import java.util.List;
import java.util.Optional;

public interface StationRepository {
    Optional<Station> findById(Long id);
    Optional<Station> findByCode(String code);
    boolean existsByCode(String code);
    List<Station> findAll();
    Station save(Station station);
}
