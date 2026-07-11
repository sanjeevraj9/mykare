package com.appointementservice.dto;

import com.appointementservice.entity.NotificationStatus;
import jakarta.validation.constraints.NotNull;

public record NotificationStatusUpdateRequest(
        @NotNull NotificationStatus status,
        String message
) {
}
