package com.appointementservice.dto;

import com.appointementservice.entity.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentEvent(
        Long appointmentId,
        String patientName,
        String patientEmail,
        String doctorName,
        LocalDateTime appointmentTime,
        AppointmentStatus status,
        String eventType
) {
}
