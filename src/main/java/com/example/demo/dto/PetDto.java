package com.example.demo.dto;

import com.example.demo.model.PetType;

/**
 * DTO for representing a pet.
 * <p>
 * Contains the necessary fields to represent a pet entry.
 *
 * @param id     the unique identifier of the pet
 * @param name   the name of the pet
 * @param age    the age of the pet
 * @param type   the type of pet (e.g., dog, cat)
 * @param address the address of the pet
 */
public record PetDto(
    Long id,
    String name,
    Integer age,
    PetType type,
    boolean deceased
) {}
