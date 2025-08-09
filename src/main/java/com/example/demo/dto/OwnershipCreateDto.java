package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

public record OwnershipCreateDto(
        @NotNull Long userId,
        @NotNull Long petId) {
}
