package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressCreateDto(
        @NotBlank String city,
        @NotBlank String type,
        @NotBlank String addressName,
        @NotBlank String number) {
}