package com.example.demo.dto;

/**
 * DTO for representing an address.
 * <p>
 * Contains the necessary fields to represent an address entry.
 *
 * @param id          the unique identifier of the address
 * @param city        the city where the address is located
 * @param type        the type of address (e.g., home, work)
 * @param addressName the name or description of the address
 * @param number      the address number
 */
public record AddressDto(
        Long id,
        String city,
        String type,
        String addressName,
        String number) {
}
