package com.example.demo.dto;

import com.example.demo.model.Gender;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateDto(
        @NotBlank String name,
        @NotBlank String firstName,
        @Min(0) @Max(150) Integer age,
        @NotNull Gender gender,
        @NotNull AddressCreateDto address) {
}
