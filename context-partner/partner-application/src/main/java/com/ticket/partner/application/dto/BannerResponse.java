package com.ticket.partner.application.dto;

import com.ticket.partner.domain.model.BannerPosition;
import com.ticket.partner.domain.model.BannerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private Long id;
    private Long operatorId;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private BannerPosition position;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer priority;
    private BannerStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
