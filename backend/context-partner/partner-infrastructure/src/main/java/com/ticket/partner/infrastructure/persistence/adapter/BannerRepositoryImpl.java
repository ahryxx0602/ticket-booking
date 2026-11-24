package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.Banner;
import com.ticket.partner.domain.model.BannerStatus;
import com.ticket.partner.domain.port.BannerRepository;
import com.ticket.partner.infrastructure.persistence.entity.BannerJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BannerRepositoryImpl implements BannerRepository {

    private final SpringDataBannerRepository repository;

    @Override
    public Optional<Banner> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<Banner> findByOperatorId(Long operatorId) {
        return repository.findByOperatorId(operatorId).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Banner> findActiveBanners(LocalDateTime now, int limit) {
        return repository.findActiveBanners(now, PageRequest.of(0, limit)).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Banner> findByStatus(BannerStatus status) {
        return repository.findByStatus(status).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Banner> findAll() {
        return repository.findAll().stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Banner save(Banner banner) {
        BannerJpaEntity entity = PartnerEntityMapper.toEntity(banner);
        BannerJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
        repository.flush();
    }
}
