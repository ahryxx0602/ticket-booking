package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.domain.model.WithdrawalStatus;
import com.ticket.partner.infrastructure.persistence.entity.WithdrawalRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataWithdrawalRequestRepository extends JpaRepository<WithdrawalRequestJpaEntity, Long> {
    List<WithdrawalRequestJpaEntity> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);
    List<WithdrawalRequestJpaEntity> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status);
}
