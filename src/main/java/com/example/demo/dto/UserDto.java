package com.example.demo.dto;

import com.example.demo.model.Gender;

public record UserDto(
                Long id,
                String name,
                String firstName,
                Integer age,
                Gender gender,
                AddressDto address,
                boolean deceased) {
}
