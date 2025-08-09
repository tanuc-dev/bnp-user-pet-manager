package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;
import com.example.demo.repository.UserPetOwnershipRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing user-pet ownership relationships.
 * Provides methods to save ownership records and query ownerships by user, pet, pet type, and city.
 *
 * <ul>
 *   <li>{@link #save(UserPetOwnership)} - Persists a UserPetOwnership entity.</li>
 *   <li>{@link #byUser(User)} - Retrieves all ownerships for a given user.</li>
 *   <li>{@link #byPet(Pet)} - Retrieves all ownerships for a given pet.</li>
 *   <li>{@link #usersByPetTypeAndCity(PetType, String)} - Finds distinct users who own pets of a specific type in a given city.</li>
 * </ul>
 *
 * Dependencies:
 * <ul>
 *   <li>{@link UserPetOwnershipRepository} - Repository for UserPetOwnership entities.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserPetOwnershipService {
    private final UserPetOwnershipRepository repo;

    public UserPetOwnership save(UserPetOwnership o) {
        return repo.save(o);
    }

    public List<UserPetOwnership> byUser(User u) {
        return repo.findByUser(u);
    }

    public List<UserPetOwnership> byPet(Pet p) {
        return repo.findByPet(p);
    }

    public List<User> usersByPetTypeAndCity(PetType type, String city) {
        return repo.findDistinctUsersByPetTypeAndCity(type, city);
    }
}
