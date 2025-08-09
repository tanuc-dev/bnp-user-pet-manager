package com.example.demo.dto;

import com.example.demo.model.Gender;

/**
 * DTO for representing a user.
 * <p>
 * Contains the necessary fields to represent a user entry.
 *
 * @param id          the unique identifier of the user
 * @param name        the name of the user
 * @param firstName   the first name of the user
 */
public record UserDto(
                Long id,
                String name,
                String firstName,
                Integer age,
                Gender gender,
                AddressDto address,
                boolean deceased) {
}
