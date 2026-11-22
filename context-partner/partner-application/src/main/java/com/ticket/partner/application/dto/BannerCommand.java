package com.ticket.partner.application.dto;

import com.ticket.partner.domain.model.BannerPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerCommand {
    private String title;
    private String imageUrl;
    private String targetUrl;
    private BannerPosition position;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer priority;
}
