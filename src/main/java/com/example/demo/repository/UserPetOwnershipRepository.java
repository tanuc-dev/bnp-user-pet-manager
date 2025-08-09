package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;

/**
 * Repository interface for managing {@link UserPetOwnership} entities.
 * <p>
 * Provides methods to perform CRUD operations and custom queries on User-Pet ownership data.
 * </p>
 */
public interface UserPetOwnershipRepository extends JpaRepository<UserPetOwnership, Long> {

    /**
     * Retrieves a list of {@link UserPetOwnership} entities associated with the specified user.
     *
     * @param user the user to search for
     * @return a list of matching {@link UserPetOwnership} entities
     */
    List<UserPetOwnership> findByUser(User user);

    /**
     * Retrieves a list of {@link UserPetOwnership} entities associated with the specified pet.
     *
     * @param pet the pet to search for
     * @return a list of matching {@link UserPetOwnership} entities
     */
    List<UserPetOwnership> findByPet(Pet pet);

    /**
     * Finds distinct users who own pets of a specific type in a given city.
     * <p>
     * @param type the type of pet to search for
     * @param city the city to search for (case-insensitive)
     * @return a list of distinct users who own pets of the specified type in the given city
     */
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