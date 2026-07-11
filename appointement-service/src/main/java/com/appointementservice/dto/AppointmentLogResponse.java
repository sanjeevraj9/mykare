package com.appointementservice.dto;

import java.time.Instant;

public record AppointmentLogResponse(
        Long id,
        Long appointmentId,
        String action,
        String message,
        Instant createdAt
) {
}
