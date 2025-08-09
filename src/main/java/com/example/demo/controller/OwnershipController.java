package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddressDto;
import com.example.demo.dto.OwnershipCreateDto;
import com.example.demo.dto.PetDto;
import com.example.demo.dto.UserDto;
import com.example.demo.model.Address;
import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;
import com.example.demo.service.PetService;
import com.example.demo.service.UserPetOwnershipService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ownerships")
@RequiredArgsConstructor
public class OwnershipController {

    private final UserService userService;
    private final PetService petService;
    private final UserPetOwnershipService ownershipService;

    // Link a user + pet at a specific address (address is de-duped)
    @PostMapping
    public void link(@Valid @RequestBody OwnershipCreateDto dto) {
        User user = userService.getOrThrow(dto.userId());
        Pet pet = petService.getOrThrow(dto.petId());
        if (!user.getAddress().getId().equals(pet.getAddress().getId())) {
            throw new IllegalArgumentException("Co-ownership allowed only for users at the pet's address.");
        }
        ownershipService.save(UserPetOwnership.builder()
                .user(user).pet(pet).build());
    }

    // 1) Pets owned by a user (handle homonyms)
    @GetMapping("/pets-by-user")
    public List<PetDto> petsByUser(@RequestParam String name, @RequestParam String firstName) {
        return userService.byNameFirstName(name, firstName).stream()
                .flatMap(u -> ownershipService.byUser(u).stream())
                .map(UserPetOwnership::getPet)
                .filter(p -> !p.isDeceased())
                .distinct()
                .map(p -> new PetDto(p.getId(), p.getName(), p.getAge(), p.getType(), p.isDeceased()))
                .toList();
    }

    // 2) Pets from a specific city
    @GetMapping("/pets-by-city")
    public List<PetDto> petsByCity(@RequestParam String city) {
        return petService.byCity(city).stream()
                .map(this::toPetDto)
                .toList();
    }

    // 3) Users that own a specific kind of pet from a specific city
    @GetMapping("/users-by-pet-type-and-city")
    public List<UserDto> usersByPetTypeAndCity(@RequestParam PetType petType, @RequestParam String city) {
        return ownershipService.usersByPetTypeAndCity(petType, city).stream()
                .map(this::toUserDto)
                .toList();
    }

    // 4) Pets owned by women in a city
    @GetMapping("/pets-by-women-in-city")
    public List<PetDto> petsByWomenInCity(@RequestParam String city) {
        // women in city → get their pets
        return userService.womenInCity(city).stream()
                .flatMap(u -> ownershipService.byUser(u).stream())
                .map(UserPetOwnership::getPet)
                .filter(p -> !p.isDeceased())
                .distinct()
                .map(p -> new PetDto(p.getId(), p.getName(), p.getAge(), p.getType(), p.isDeceased()))
                .toList();
    }

    // ---- mappers ----
    private UserDto toUserDto(User u) {
        Address a = u.getAddress();
        var ad = new AddressDto(a.getId(), a.getCity(), a.getType(), a.getAddressName(), a.getNumber());
        return new UserDto(u.getId(), u.getName(), u.getFirstName(), u.getAge(), u.getGender(), ad, u.isDeceased());
    }

    private PetDto toPetDto(Pet p) {
        return new PetDto(p.getId(), p.getName(), p.getAge(), p.getType(), p.isDeceased());
    }
}

