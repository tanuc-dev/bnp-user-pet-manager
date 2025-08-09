package com.example.demo.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.PetCreateDto;
import com.example.demo.dto.PetDto;
import com.example.demo.model.Address;
import com.example.demo.model.Pet;
import com.example.demo.service.AddressService;
import com.example.demo.service.PetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing pets.
 * <p>
 * Provides endpoints to create, update, and mark pets as deceased.
 * </p>
 *
 * <ul>
 *   <li><b>POST /pets</b>: Create a new pet.</li>
 *   <li><b>PUT /pets/{id}</b>: Update an existing pet with pessimistic locking and retry.</li>
 *   <li><b>PATCH /pets/{id}/death</b>: Mark a pet as deceased.</li>
 * </ul>
 *
 * Dependencies:
 * <ul>
 *   <li>{@link PetService} - Service for pet operations.</li>
 *   <li>{@link AddressService} - Service for address operations.</li>
 * </ul>
 *
 * All endpoints return {@link PetDto} objects.
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final AddressService addressService;

    /**
     * Creates a new pet.
     *
     * @param dto the pet creation data transfer object
     * @return the created pet data transfer object
     */
    @PostMapping
    public PetDto create(@Valid @RequestBody PetCreateDto dto) {
        Address addr = addressService.findOrCreate(dto.address());
        Pet saved = petService.save(Pet.builder()
                .name(dto.name())
                .age(dto.age())
                .type(dto.type())
                .address(addr)
                .build());
        return toDto(saved);
    }

    /**
     * Updates an existing pet.
     *
     * @param id  the ID of the pet to update
     * @param dto the pet update data transfer object
     * @return the updated pet data transfer object
     */
    @PutMapping("/{id}")
    public PetDto update(@PathVariable Long id, @Valid @RequestBody PetCreateDto dto) {
        Pet updated = petService.updateWithPessimisticLockAndRetry(id, p -> {
            p.setName(dto.name());
            p.setAge(dto.age());
            p.setType(dto.type());
            var addr = addressService.findOrCreate(dto.address());
            p.setAddress(addr);
        });
        return toDto(updated);
    }

    /**
     * Marks a pet as deceased.
     *
     * @param id the ID of the pet to mark as deceased
     * @return the updated pet data transfer object
     */
    @PatchMapping("/{id}/death")
    public PetDto markDeceased(@PathVariable Long id) {
        return toDto(petService.markDeceased(id));
    }

    private PetDto toDto(Pet p) {
        return new PetDto(p.getId(), p.getName(), p.getAge(), p.getType(), p.isDeceased());
    }
}
