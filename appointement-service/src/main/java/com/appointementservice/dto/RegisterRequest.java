package com.appointementservice.dto;

import com.appointementservice.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 6, message = "password must be at least 6 characters") String password,
        @NotNull UserRole role
) {
}
