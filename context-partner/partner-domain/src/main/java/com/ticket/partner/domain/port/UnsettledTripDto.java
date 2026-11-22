package com.ticket.partner.domain.port;

import java.time.LocalDateTime;

public record UnsettledTripDto(
        Long tripId,
        Long operatorId,
        LocalDateTime departureTime
) {}
