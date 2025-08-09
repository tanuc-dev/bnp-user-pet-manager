package com.example.demo.dto;

import com.example.demo.model.PetType;

public record PetDto(
    Long id,
    String name,
    Integer age,
    PetType type,
    boolean deceased
) {}
