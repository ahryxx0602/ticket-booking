package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.infrastructure.persistence.entity.OperatorWalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface SpringDataOperatorWalletRepository extends JpaRepository<OperatorWalletJpaEntity, Long> {
    Optional<OperatorWalletJpaEntity> findByOperatorId(Long operatorId);

    @Modifying
    @Query("UPDATE OperatorWalletJpaEntity w SET w.pendingSettlementBalance = w.pendingSettlementBalance + :amount WHERE w.operatorId = :operatorId")
    void creditPendingAtomic(@Param("operatorId") Long operatorId, @Param("amount") BigDecimal amount);
}
