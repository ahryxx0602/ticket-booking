package com.ticket.partner.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;

    public static final String KEY_GLOBAL_COMMISSION_RATE = "global_commission_rate";
    public static final String KEY_PLATFORM_LOGO_URL = "platform_logo_url";
    public static final String KEY_MIN_WITHDRAWAL_AMOUNT = "min_withdrawal_amount";
}
