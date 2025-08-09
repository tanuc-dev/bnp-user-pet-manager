package com.example.demo.dto;

import com.example.demo.model.PetType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PetCreateDto(
        @NotBlank String name,
        @Min(0) @Max(200) Integer age,
        @NotNull PetType type,
        @NotNull AddressCreateDto address) {
}