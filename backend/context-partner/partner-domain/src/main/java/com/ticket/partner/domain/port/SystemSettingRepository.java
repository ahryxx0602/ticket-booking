package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.SystemSetting;

import java.util.List;
import java.util.Optional;

public interface SystemSettingRepository {
    Optional<SystemSetting> findByKey(String settingKey);
    List<SystemSetting> findAll();
    SystemSetting save(SystemSetting setting);
}
