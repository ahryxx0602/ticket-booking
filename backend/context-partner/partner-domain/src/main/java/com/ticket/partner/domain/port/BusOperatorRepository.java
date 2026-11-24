package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.BusOperator;
import com.ticket.partner.domain.model.OperatorStatus;

import java.util.List;
import java.util.Optional;

public interface BusOperatorRepository {
    Optional<BusOperator> findById(Long id);
    Optional<BusOperator> findByUserId(Long userId);
    List<BusOperator> findByStatus(OperatorStatus status);
    List<BusOperator> findAll();
    boolean existsByTaxCode(String taxCode);
    BusOperator save(BusOperator operator);
    void deleteById(Long id);
}
