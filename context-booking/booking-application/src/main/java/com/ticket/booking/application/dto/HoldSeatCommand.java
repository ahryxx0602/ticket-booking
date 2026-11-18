package com.ticket.booking.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class HoldSeatCommand {
    @NotNull
    private Long userId;

    @NotNull
    private Long tripId;

    @NotEmpty
    private List<String> seatNumbers;
}
