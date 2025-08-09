package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;
import com.example.demo.repository.UserPetOwnershipRepository;

import lombok.RequiredArgsConstructor;

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
