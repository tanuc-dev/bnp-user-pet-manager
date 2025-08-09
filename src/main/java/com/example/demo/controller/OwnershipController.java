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

/**
 * REST controller for managing user-pet ownerships.
 * <p>
 * Provides endpoints to:
 * <ul>
 *     <li>Link a user and a pet at a specific address (address is de-duped).</li>
 *     <li>Retrieve pets owned by a user, handling homonyms.</li>
 *     <li>Retrieve pets from a specific city.</li>
 *     <li>Retrieve users that own a specific kind of pet from a specific city.</li>
 *     <li>Retrieve pets owned by women in a city.</li>
 * </ul>
 * <p>
 * Utilizes {@link UserService}, {@link PetService}, and {@link UserPetOwnershipService}
 * for business logic and data access.
 */
@RestController
@RequestMapping("/ownerships")
@RequiredArgsConstructor
public class OwnershipController {

    private final UserService userService;
    private final PetService petService;
    private final UserPetOwnershipService ownershipService;

    /**
     * Links a user and a pet at a specific address.
     *
     * @param dto the ownership creation data transfer object
     */
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

    /**
     * Retrieves pets owned by a user, handling homonyms.
     *
     * @param name      the name of the user
     * @param firstName the first name of the user
     * @return a list of pets owned by the user
     */
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

    /**
     * Retrieves pets from a specific city.
     *
     * @param city the city to search for pets
     * @return a list of pets in the specified city
     */
    @GetMapping("/pets-by-city")
    public List<PetDto> petsByCity(@RequestParam String city) {
        return petService.byCity(city).stream()
                .map(this::toPetDto)
                .toList();
    }

    /**
     * Retrieves users that own a specific kind of pet from a specific city.
     *
     * @param petType the type of pet
     * @param city    the city to search for users
     * @return a list of users that own the specified pet type in the specified city
     */
    @GetMapping("/users-by-pet-type-and-city")
    public List<UserDto> usersByPetTypeAndCity(@RequestParam PetType petType, @RequestParam String city) {
        return ownershipService.usersByPetTypeAndCity(petType, city).stream()
                .map(this::toUserDto)
                .toList();
    }

    /**
     * Retrieves pets owned by women in a specific city.
     *
     * @param city the city to search for pets
     * @return a list of pets owned by women in the specified city
     */
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

    /**
     * Maps a User entity to a UserDto.
     *
     * @param u the User entity
     * @return the mapped UserDto
     */
    private UserDto toUserDto(User u) {
        Address a = u.getAddress();
        var ad = new AddressDto(a.getId(), a.getCity(), a.getType(), a.getAddressName(), a.getNumber());
        return new UserDto(u.getId(), u.getName(), u.getFirstName(), u.getAge(), u.getGender(), ad, u.isDeceased());
    }

    /**
     * Maps a Pet entity to a PetDto.
     *
     * @param p the Pet entity
     * @return the mapped PetDto
     */
    private PetDto toPetDto(Pet p) {
        return new PetDto(p.getId(), p.getName(), p.getAge(), p.getType(), p.isDeceased());
    }
}

