package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverRepository {
    Optional<Driver> findById(Long id);
    Optional<Driver> findByIdAndOperatorId(Long id, Long operatorId);
    List<Driver> findByOperatorId(Long operatorId);
    Driver save(Driver driver);
}
