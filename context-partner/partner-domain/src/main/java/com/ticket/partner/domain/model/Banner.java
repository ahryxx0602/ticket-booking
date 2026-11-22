package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner extends BaseEntity {
    private Long id;
    private Long operatorId; // null = platform banner
    private String title;
    private String imageUrl;
    private String targetUrl;
    private BannerPosition position;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Builder.Default
    private Integer priority = 0;
    private BannerStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public void validate() {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("INVALID_BANNER_TITLE", "Banner title cannot be blank");
        }
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BusinessException("INVALID_BANNER_IMAGE", "Banner image URL cannot be blank");
        }
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new BusinessException("INVALID_BANNER_DATES", "End date must be strictly after start date");
        }
        sanitizeTargetUrl();
    }

    private void sanitizeTargetUrl() {
        if (targetUrl != null) {
            String lower = targetUrl.trim().toLowerCase();
            if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
                throw new BusinessException("DANGEROUS_TARGET_URL", "Target URL contains potentially malicious protocol");
            }
        }
    }

    public void approve() {
        this.status = BannerStatus.ACTIVE;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.status = BannerStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
