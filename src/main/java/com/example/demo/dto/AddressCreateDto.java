package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new address.
 * <p>
 * Contains the necessary fields required to create an address entry.
 *
 * @param city        the city where the address is located; must not be blank
 * @param type        the type of address (e.g., home, work); must not be blank
 * @param addressName the name or description of the address; must not be blank
 * @param number      the address number; must not be blank
 */
public record AddressCreateDto(
        @NotBlank String city,
        @NotBlank String type,
        @NotBlank String addressName,
        @NotBlank String number) {
}