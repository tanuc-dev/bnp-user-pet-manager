package com.example.demo.dto;

import com.example.demo.model.PetType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new pet.
 * <p>
 * Contains the necessary fields required to create a pet entry.
 *
 * @param name  the name of the pet; must not be blank
 * @param age   the age of the pet; must be between 0 and 200
 * @param type  the type of pet (e.g., dog, cat); must not be null
 * @param address the address of the pet; must not be null
 */
public record PetCreateDto(
        @NotBlank String name,
        @Min(0) @Max(200) Integer age,
        @NotNull PetType type,
        @NotNull AddressCreateDto address) {
}