package com.example.demo.dto;

public record AddressDto(
        Long id,
        String city,
        String type,
        String addressName,
        String number) {
}
