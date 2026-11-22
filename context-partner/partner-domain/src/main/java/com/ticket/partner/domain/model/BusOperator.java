package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusOperator extends BaseEntity {
    private Long id;
    private Long userId;
    private String businessName;
    private String brandName;
    private String taxCode;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String description;
    private BigDecimal commissionRate; // null = dùng global rate sàn
    private OperatorStatus status;
    private String rejectionReason;
    private Long version;

    public void activate() {
        if (this.status == OperatorStatus.ACTIVE) {
            return;
        }
        this.status = OperatorStatus.ACTIVE;
        this.rejectionReason = null;
    }

    public void suspend(String reason) {
        this.status = OperatorStatus.SUSPENDED;
        this.rejectionReason = reason;
    }

    public void reject(String reason) {
        if (this.status != OperatorStatus.PENDING_APPROVAL) {
            throw new BusinessException("INVALID_OPERATOR_STATUS", "Only PENDING_APPROVAL operators can be rejected");
        }
        this.status = OperatorStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void updateCommissionRate(BigDecimal rate) {
        if (rate != null && (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException("INVALID_COMMISSION_RATE", "Commission rate must be between 0% and 100%");
        }
        this.commissionRate = rate;
    }

    public void updateProfile(String brandName, String contactPhone, String contactEmail, String logoUrl, String description) {
        if (brandName != null && !brandName.trim().isEmpty()) {
            this.brandName = brandName.trim();
        }
        if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            this.contactPhone = contactPhone.trim();
        }
        if (contactEmail != null && !contactEmail.trim().isEmpty()) {
            this.contactEmail = contactEmail.trim();
        }
        if (logoUrl != null) {
            this.logoUrl = logoUrl.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
    }
}
