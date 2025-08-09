package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;

public interface UserPetOwnershipRepository extends JpaRepository<UserPetOwnership, Long> {
    List<UserPetOwnership> findByUser(User user);

    List<UserPetOwnership> findByPet(Pet pet);

    @Query("""
                select distinct u
                from UserPetOwnership o
                join o.user u
                join o.pet p
                join p.address a
                where p.type = :type
                  and lower(a.city) = lower(:city)
                  and u.deceased = false
                  and p.deceased = false
            """)
    List<User> findDistinctUsersByPetTypeAndCity(
            @Param("type") PetType type,
            @Param("city") String city);
}