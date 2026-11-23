package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.Banner;
import com.ticket.partner.domain.model.BannerStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BannerRepository {
    Optional<Banner> findById(Long id);
    List<Banner> findByOperatorId(Long operatorId);
    List<Banner> findActiveBanners(LocalDateTime now, int limit);
    List<Banner> findByStatus(BannerStatus status);
    Banner save(Banner banner);
}
