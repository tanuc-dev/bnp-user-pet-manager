package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new ownership.
 * <p>
 * Contains the necessary fields required to create an ownership entry.
 *
 * @param userId the ID of the user
 * @param petId  the ID of the pet
 */
public record OwnershipCreateDto(
        @NotNull Long userId,
        @NotNull Long petId) {
}
