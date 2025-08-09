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

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final AddressService addressService;

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

    @PatchMapping("/{id}/death")
    public PetDto markDeceased(@PathVariable Long id) {
        return toDto(petService.markDeceased(id));
    }

    private PetDto toDto(Pet p) {
        return new PetDto(p.getId(), p.getName(), p.getAge(), p.getType(), p.isDeceased());
    }
}
