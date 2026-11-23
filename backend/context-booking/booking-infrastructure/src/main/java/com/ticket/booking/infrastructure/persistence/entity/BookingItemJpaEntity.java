package com.ticket.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
@Getter
@Setter
public class BookingItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingJpaEntity booking;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
