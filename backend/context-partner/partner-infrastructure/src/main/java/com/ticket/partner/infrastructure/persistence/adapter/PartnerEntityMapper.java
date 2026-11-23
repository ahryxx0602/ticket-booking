package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.*;
import com.ticket.partner.infrastructure.persistence.entity.*;

public final class PartnerEntityMapper {

    private PartnerEntityMapper() {}

    public static BusOperator toDomain(BusOperatorJpaEntity entity) {
        if (entity == null) return null;
        BusOperator op = BusOperator.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .businessName(entity.getBusinessName())
                .brandName(entity.getBrandName())
                .taxCode(entity.getTaxCode())
                .contactPhone(entity.getContactPhone())
                .contactEmail(entity.getContactEmail())
                .logoUrl(entity.getLogoUrl())
                .description(entity.getDescription())
                .commissionRate(entity.getCommissionRate())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .version(entity.getVersion())
                .build();
        op.setCreatedAt(entity.getCreatedAt());
        op.setUpdatedAt(entity.getUpdatedAt());
        return op;
    }

    public static BusOperatorJpaEntity toEntity(BusOperator domain) {
        if (domain == null) return null;
        return BusOperatorJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .businessName(domain.getBusinessName())
                .brandName(domain.getBrandName())
                .taxCode(domain.getTaxCode())
                .contactPhone(domain.getContactPhone())
                .contactEmail(domain.getContactEmail())
                .logoUrl(domain.getLogoUrl())
                .description(domain.getDescription())
                .commissionRate(domain.getCommissionRate())
                .status(domain.getStatus())
                .rejectionReason(domain.getRejectionReason())
                .version(domain.getVersion() != null ? domain.getVersion() : 0L)
                .build();
    }

    public static Vehicle toDomain(VehicleJpaEntity entity) {
        if (entity == null) return null;
        Vehicle v = Vehicle.builder()
                .id(entity.getId())
                .operatorId(entity.getOperatorId())
                .plateNumber(entity.getPlateNumber())
                .modelName(entity.getModelName())
                .vehicleType(entity.getVehicleType())
                .totalSeats(entity.getTotalSeats())
                .seatRows(entity.getSeatRows())
                .seatsPerRow(entity.getSeatsPerRow())
                .vipRows(entity.getVipRows())
                .status(entity.getStatus())
                .build();
        v.setCreatedAt(entity.getCreatedAt());
        v.setUpdatedAt(entity.getUpdatedAt());
        return v;
    }

    public static VehicleJpaEntity toEntity(Vehicle domain) {
        if (domain == null) return null;
        return VehicleJpaEntity.builder()
                .id(domain.getId())
                .operatorId(domain.getOperatorId())
                .plateNumber(domain.getPlateNumber())
                .modelName(domain.getModelName())
                .vehicleType(domain.getVehicleType())
                .totalSeats(domain.getTotalSeats())
                .seatRows(domain.getSeatRows())
                .seatsPerRow(domain.getSeatsPerRow())
                .vipRows(domain.getVipRows())
                .status(domain.getStatus())
                .build();
    }

    public static Driver toDomain(DriverJpaEntity entity) {
        if (entity == null) return null;
        Driver d = Driver.builder()
                .id(entity.getId())
                .operatorId(entity.getOperatorId())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .licenseNumber(entity.getLicenseNumber())
                .status(entity.getStatus())
                .build();
        d.setCreatedAt(entity.getCreatedAt());
        d.setUpdatedAt(entity.getUpdatedAt());
        return d;
    }

    public static DriverJpaEntity toEntity(Driver domain) {
        if (domain == null) return null;
        return DriverJpaEntity.builder()
                .id(domain.getId())
                .operatorId(domain.getOperatorId())
                .fullName(domain.getFullName())
                .phone(domain.getPhone())
                .licenseNumber(domain.getLicenseNumber())
                .status(domain.getStatus())
                .build();
    }

    public static OperatorWallet toDomain(OperatorWalletJpaEntity entity) {
        if (entity == null) return null;
        OperatorWallet w = OperatorWallet.builder()
                .id(entity.getId())
                .operatorId(entity.getOperatorId())
                .availableBalance(entity.getAvailableBalance())
                .pendingSettlementBalance(entity.getPendingSettlementBalance())
                .lockedBalance(entity.getLockedBalance())
                .debtBalance(entity.getDebtBalance())
                .currency(entity.getCurrency())
                .version(entity.getVersion())
                .build();
        w.setCreatedAt(entity.getCreatedAt());
        w.setUpdatedAt(entity.getUpdatedAt());
        return w;
    }

    public static OperatorWalletJpaEntity toEntity(OperatorWallet domain) {
        if (domain == null) return null;
        return OperatorWalletJpaEntity.builder()
                .id(domain.getId())
                .operatorId(domain.getOperatorId())
                .availableBalance(domain.getAvailableBalance())
                .pendingSettlementBalance(domain.getPendingSettlementBalance())
                .lockedBalance(domain.getLockedBalance())
                .debtBalance(domain.getDebtBalance())
                .currency(domain.getCurrency() != null ? domain.getCurrency() : "VND")
                .version(domain.getVersion() != null ? domain.getVersion() : 0L)
                .build();
    }

    public static WalletTransaction toDomain(WalletTransactionJpaEntity entity) {
        if (entity == null) return null;
        WalletTransaction tx = WalletTransaction.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .operatorId(entity.getOperatorId())
                .bookingId(entity.getBookingId())
                .tripId(entity.getTripId())
                .paymentReference(entity.getPaymentReference())
                .transactionType(entity.getTransactionType())
                .grossAmount(entity.getGrossAmount())
                .commissionRate(entity.getCommissionRate())
                .commissionAmount(entity.getCommissionAmount())
                .netAmount(entity.getNetAmount())
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
        tx.setCreatedAt(entity.getCreatedAt());
        return tx;
    }

    public static WalletTransactionJpaEntity toEntity(WalletTransaction domain) {
        if (domain == null) return null;
        return WalletTransactionJpaEntity.builder()
                .id(domain.getId())
                .walletId(domain.getWalletId())
                .operatorId(domain.getOperatorId())
                .bookingId(domain.getBookingId())
                .tripId(domain.getTripId())
                .paymentReference(domain.getPaymentReference())
                .transactionType(domain.getTransactionType())
                .grossAmount(domain.getGrossAmount())
                .commissionRate(domain.getCommissionRate())
                .commissionAmount(domain.getCommissionAmount())
                .netAmount(domain.getNetAmount())
                .balanceBefore(domain.getBalanceBefore())
                .balanceAfter(domain.getBalanceAfter())
                .description(domain.getDescription())
                .build();
    }

    public static WithdrawalRequest toDomain(WithdrawalRequestJpaEntity entity) {
        if (entity == null) return null;
        WithdrawalRequest req = WithdrawalRequest.builder()
                .id(entity.getId())
                .operatorId(entity.getOperatorId())
                .amount(entity.getAmount())
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .accountHolderName(entity.getAccountHolderName())
                .status(entity.getStatus())
                .adminNote(entity.getAdminNote())
                .processedByAdminId(entity.getProcessedByAdminId())
                .processedAt(entity.getProcessedAt())
                .idempotencyKey(entity.getIdempotencyKey())
                .createdAt(entity.getCreatedAt())
                .build();
        req.setCreatedAt(entity.getCreatedAt());
        req.setUpdatedAt(entity.getUpdatedAt());
        return req;
    }

    public static WithdrawalRequestJpaEntity toEntity(WithdrawalRequest domain) {
        if (domain == null) return null;
        return WithdrawalRequestJpaEntity.builder()
                .id(domain.getId())
                .operatorId(domain.getOperatorId())
                .amount(domain.getAmount())
                .bankName(domain.getBankName())
                .accountNumber(domain.getAccountNumber())
                .accountHolderName(domain.getAccountHolderName())
                .status(domain.getStatus())
                .adminNote(domain.getAdminNote())
                .processedByAdminId(domain.getProcessedByAdminId())
                .processedAt(domain.getProcessedAt())
                .idempotencyKey(domain.getIdempotencyKey())
                .build();
    }

    public static Banner toDomain(BannerJpaEntity entity) {
        if (entity == null) return null;
        Banner b = Banner.builder()
                .id(entity.getId())
                .operatorId(entity.getOperatorId())
                .title(entity.getTitle())
                .imageUrl(entity.getImageUrl())
                .targetUrl(entity.getTargetUrl())
                .position(entity.getPosition())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .build();
        b.setCreatedAt(entity.getCreatedAt());
        b.setUpdatedAt(entity.getUpdatedAt());
        return b;
    }

    public static BannerJpaEntity toEntity(Banner domain) {
        if (domain == null) return null;
        return BannerJpaEntity.builder()
                .id(domain.getId())
                .operatorId(domain.getOperatorId())
                .title(domain.getTitle())
                .imageUrl(domain.getImageUrl())
                .targetUrl(domain.getTargetUrl())
                .position(domain.getPosition())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .priority(domain.getPriority() != null ? domain.getPriority() : 0)
                .status(domain.getStatus())
                .rejectionReason(domain.getRejectionReason())
                .build();
    }

    public static SystemSetting toDomain(SystemSettingJpaEntity entity) {
        if (entity == null) return null;
        return SystemSetting.builder()
                .settingKey(entity.getSettingKey())
                .settingValue(entity.getSettingValue())
                .description(entity.getDescription())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static SystemSettingJpaEntity toEntity(SystemSetting domain) {
        if (domain == null) return null;
        return SystemSettingJpaEntity.builder()
                .settingKey(domain.getSettingKey())
                .settingValue(domain.getSettingValue())
                .description(domain.getDescription())
                .build();
    }
}
