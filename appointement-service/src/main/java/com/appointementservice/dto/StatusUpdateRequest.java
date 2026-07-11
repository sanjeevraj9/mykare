package com.appointementservice.dto;

import com.appointementservice.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull AppointmentStatus status) {
}
