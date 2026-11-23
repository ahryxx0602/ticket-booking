package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.SystemSetting;
import com.ticket.partner.domain.port.SystemSettingRepository;
import com.ticket.partner.infrastructure.persistence.entity.SystemSettingJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataSystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SystemSettingRepositoryImpl implements SystemSettingRepository {

    private final SpringDataSystemSettingRepository repository;

    @Override
    public Optional<SystemSetting> findByKey(String settingKey) {
        return repository.findBySettingKey(settingKey).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<SystemSetting> findAll() {
        return repository.findAll().stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public SystemSetting save(SystemSetting setting) {
        SystemSettingJpaEntity entity = repository.findBySettingKey(setting.getSettingKey())
                .orElseGet(() -> PartnerEntityMapper.toEntity(setting));
        entity.setSettingValue(setting.getSettingValue());
        if (setting.getDescription() != null) {
            entity.setDescription(setting.getDescription());
        }
        SystemSettingJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }
}
