package com.appointementservice.dto;

import com.appointementservice.entity.UserRole;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        String email,
        UserRole role
) {
}
