package com.ticket.partner.application.service;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.domain.model.*;

public final class PartnerMapper {

    private PartnerMapper() {}

    public static OperatorResponse toResponse(BusOperator operator) {
        if (operator == null) return null;
        return OperatorResponse.builder()
                .id(operator.getId())
                .userId(operator.getUserId())
                .businessName(operator.getBusinessName())
                .brandName(operator.getBrandName())
                .taxCode(operator.getTaxCode())
                .contactPhone(operator.getContactPhone())
                .contactEmail(operator.getContactEmail())
                .logoUrl(operator.getLogoUrl())
                .description(operator.getDescription())
                .commissionRate(operator.getCommissionRate())
                .status(operator.getStatus())
                .rejectionReason(operator.getRejectionReason())
                .createdAt(operator.getCreatedAt())
                .build();
    }

    public static VehicleResponse toResponse(Vehicle vehicle) {
        if (vehicle == null) return null;
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .operatorId(vehicle.getOperatorId())
                .plateNumber(vehicle.getPlateNumber())
                .modelName(vehicle.getModelName())
                .vehicleType(vehicle.getVehicleType())
                .totalSeats(vehicle.getTotalSeats())
                .seatRows(vehicle.getSeatRows())
                .seatsPerRow(vehicle.getSeatsPerRow())
                .vipRows(vehicle.getVipRows())
                .status(vehicle.getStatus())
                .build();
    }

    public static DriverResponse toResponse(Driver driver) {
        if (driver == null) return null;
        return DriverResponse.builder()
                .id(driver.getId())
                .operatorId(driver.getOperatorId())
                .fullName(driver.getFullName())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .status(driver.getStatus())
                .build();
    }

    public static WithdrawalResponse toResponse(WithdrawalRequest req) {
        if (req == null) return null;
        return WithdrawalResponse.builder()
                .id(req.getId())
                .operatorId(req.getOperatorId())
                .amount(req.getAmount())
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .accountHolderName(req.getAccountHolderName())
                .status(req.getStatus())
                .adminNote(req.getAdminNote())
                .processedByAdminId(req.getProcessedByAdminId())
                .processedAt(req.getProcessedAt())
                .createdAt(req.getCreatedAt())
                .build();
    }

    public static BannerResponse toResponse(Banner banner) {
        if (banner == null) return null;
        return BannerResponse.builder()
                .id(banner.getId())
                .operatorId(banner.getOperatorId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .targetUrl(banner.getTargetUrl())
                .position(banner.getPosition())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .priority(banner.getPriority())
                .status(banner.getStatus())
                .rejectionReason(banner.getRejectionReason())
                .createdAt(banner.getCreatedAt())
                .build();
    }

    public static WalletResponse toResponse(OperatorWallet wallet) {
        if (wallet == null) return null;
        return WalletResponse.builder()
                .id(wallet.getId())
                .operatorId(wallet.getOperatorId())
                .availableBalance(wallet.getAvailableBalance())
                .pendingSettlementBalance(wallet.getPendingSettlementBalance())
                .lockedBalance(wallet.getLockedBalance())
                .debtBalance(wallet.getDebtBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    public static WalletTransactionResponse toResponse(WalletTransaction tx) {
        if (tx == null) return null;
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .walletId(tx.getWalletId())
                .operatorId(tx.getOperatorId())
                .bookingId(tx.getBookingId())
                .tripId(tx.getTripId())
                .paymentReference(tx.getPaymentReference())
                .transactionType(tx.getTransactionType())
                .grossAmount(tx.getGrossAmount())
                .commissionRate(tx.getCommissionRate())
                .commissionAmount(tx.getCommissionAmount())
                .netAmount(tx.getNetAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
