package com.appointementservice.dto;

import java.time.LocalDateTime;

public record AvailableSlotResponse(
        String doctorName,
        LocalDateTime slotTime,
        boolean available
) {
}
