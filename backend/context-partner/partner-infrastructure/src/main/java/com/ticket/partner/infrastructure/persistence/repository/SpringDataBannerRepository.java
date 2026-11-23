package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.domain.model.BannerStatus;
import com.ticket.partner.infrastructure.persistence.entity.BannerJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpringDataBannerRepository extends JpaRepository<BannerJpaEntity, Long> {
    List<BannerJpaEntity> findByOperatorId(Long operatorId);
    List<BannerJpaEntity> findByStatus(BannerStatus status);

    @Query("SELECT b FROM BannerJpaEntity b WHERE b.status = 'ACTIVE' AND b.startDate <= :now AND b.endDate >= :now ORDER BY b.priority DESC, b.createdAt DESC")
    List<BannerJpaEntity> findActiveBanners(@Param("now") LocalDateTime now, Pageable pageable);
}
