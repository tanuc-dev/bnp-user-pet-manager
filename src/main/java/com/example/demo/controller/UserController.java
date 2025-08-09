package com.example.demo.controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddressDto;
import com.example.demo.dto.UserCreateDto;
import com.example.demo.dto.UserDto;
import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.service.AddressService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    // CREATE (insert) - address is created or reused (de-dup) behind the scenes
    @PostMapping
    public UserDto create(@Valid @RequestBody UserCreateDto dto) {
        Address addr = addressService.findOrCreate(dto.address());
        User saved = userService.save(User.builder()
                .name(dto.name())
                .firstName(dto.firstName())
                .age(dto.age())
                .gender(dto.gender())
                .address(addr)
                .build());
        return toDto(saved);
    }

    // UPDATE user core fields + (optionally) move to a new address (also de-duped)
    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserCreateDto dto) {
        var updated = userService.updateWithPessimisticLockAndRetry(id, u -> {
            u.setName(dto.name());
            u.setFirstName(dto.firstName());
            u.setAge(dto.age());
            u.setGender(dto.gender());
            var addr = addressService.findOrCreate(dto.address());
            u.setAddress(addr);
        });
        return toDto(updated);

    }

    // Mark user deceased (soft delete)
    @PatchMapping("/{id}/death")
    public UserDto markDeceased(@PathVariable Long id) {
        return toDto(userService.markDeceased(id));
    }

    // Handle homonyms: returns all matching users (you can pick the correct one by id)
    @GetMapping("/by-name")
    public List<UserDto> byName(@RequestParam String name, @RequestParam String firstName) {
        return userService.byNameFirstName(name, firstName).stream().map(this::toDto).toList();
    }

    // ---- mappers ----
    private UserDto toDto(User u) {
        var a = u.getAddress();
        var ad = new AddressDto(a.getId(), a.getCity(), a.getType(), a.getAddressName(), a.getNumber());
        return new UserDto(u.getId(), u.getName(), u.getFirstName(), u.getAge(), u.getGender(), ad, u.isDeceased());
    }
}

