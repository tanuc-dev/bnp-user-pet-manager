package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

/**
 * Repository interface for managing {@link Pet} entities.
 * <p>
 * Provides methods to perform CRUD operations and custom queries on Pet data.
 * </p>
 *
 * <p>
 * Extends {@link JpaRepository} to inherit standard data access methods.
 * </p>
 *
 * <p>
 * Custom Query Methods:
 * <ul>
 *   <li>
 *     {@code findByType}:
 *     Retrieves a list of {@link Pet} entities matching the specified type.
 *   </li>
 *   <li>
 *     {@code findByAddress_CityIgnoreCaseAndDeceasedFalse}:
 *     Retrieves a list of {@link Pet} entities located in the specified city
 *     and not marked as deceased.
 *   </li>
 * </ul>
 * </p>
 */
public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * Retrieves a list of {@link Pet} entities matching the specified type.
     *
     * @param type the type of pet to search for
     * @return a list of matching {@link Pet} entities
     */
    List<Pet> findByType(PetType type);

    /**
     * Retrieves a list of {@link Pet} entities located in the specified city
     * and not marked as deceased.
     *
     * @param city the city to search for (case-insensitive)
     * @return a list of matching {@link Pet} entities
     */
    List<Pet> findByAddress_CityIgnoreCaseAndDeceasedFalse(String city);

    /**
     * Locks the specified {@link Pet} entity for update.
     *
     * @param id the ID of the pet to lock
     * @return the locked {@link Pet} entity
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id")
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") })
    Pet lockForUpdate(@Param("id") Long id);
}
