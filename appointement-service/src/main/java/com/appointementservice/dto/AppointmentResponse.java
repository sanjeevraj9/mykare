package com.appointementservice.dto;

import com.appointementservice.entity.AppointmentStatus;
import com.appointementservice.entity.NotificationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        String patientName,
        String patientEmail,
        String doctorName,
        LocalDateTime appointmentTime,
        String reason,
        String notes,
        AppointmentStatus status,
        NotificationStatus notificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
