package com.ticket.payment.infrastructure.persistence.repository;

import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, Long>, JpaSpecificationExecutor<PaymentJpaEntity> {

    Optional<PaymentJpaEntity> findByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.paymentReference = :paymentReference")
    Optional<PaymentJpaEntity> findByPaymentReferenceForUpdate(@Param("paymentReference") String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentJpaEntity> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentJpaEntity> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(CASE WHEN p.status IN (com.ticket.payment.domain.model.PaymentStatus.SUCCESS, com.ticket.payment.domain.model.PaymentStatus.REFUNDED) THEN p.amount ELSE 0 END), 0) " +
           "FROM PaymentJpaEntity p WHERE p.createdAt >= :from AND p.createdAt < :to")
    BigDecimal calculateGrossRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(CASE WHEN p.status = com.ticket.payment.domain.model.PaymentStatus.REFUNDED THEN p.refundAmount ELSE 0 END), 0) " +
           "FROM PaymentJpaEntity p WHERE p.createdAt >= :from AND p.createdAt < :to")
    BigDecimal calculateTotalRefunded(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM PaymentJpaEntity p WHERE p.createdAt >= :from AND p.createdAt < :to AND p.status = :status")
    long countByStatusInPeriod(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM PaymentJpaEntity p WHERE p.createdAt >= :from AND p.createdAt < :to")
    long countTotalInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM PaymentJpaEntity p WHERE p.createdAt >= :from AND p.createdAt < :to AND p.reconciliationRequired = true")
    long countReconciliationInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0) " +
           "FROM PaymentJpaEntity p WHERE p.createdAt >= :from AND p.createdAt < :to AND p.status IN (com.ticket.payment.domain.model.PaymentStatus.SUCCESS, com.ticket.payment.domain.model.PaymentStatus.REFUNDED) " +
           "GROUP BY p.paymentMethod")
    List<Object[]> calculateRevenueByMethod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
