package com.appointementservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotBlank String patientName,
        @Email @NotBlank String patientEmail,
        @NotBlank String doctorName,
        @NotNull @Future LocalDateTime appointmentTime,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 1000) String notes
) {
}
