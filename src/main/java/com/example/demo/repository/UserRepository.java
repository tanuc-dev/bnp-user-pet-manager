package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Gender;
import com.example.demo.model.User;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;


/**
 * Repository interface for managing {@link User} entities.
 * <p>
 * Provides methods for querying users by name, first name, gender, and city,
 * as well as a method for acquiring a pessimistic write lock on a user entity.
 * </p>
 *
 * <ul>
 *   <li>{@code findByNameAndFirstName(String name, String firstName)}: Finds users by their name and first name.</li>
 *   <li>{@code findByGenderAndAddress_CityIgnoreCase(Gender gender, String city)}: Finds users by gender and city (case-insensitive).</li>
 *   <li>{@code lockForUpdate(Long id)}: Acquires a pessimistic write lock on the user with the specified ID, with a lock timeout of 5000ms.</li>
 * </ul>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Retrieves a list of users matching the specified name and first name.
     *
     * @param name the last name of the user to search for
     * @param firstName the first name of the user to search for
     * @return a list of {@link User} entities that match the given name and first name
     */
    List<User> findByNameAndFirstName(String name, String firstName);

    /**
     * Retrieves a list of users filtered by gender and city (case-insensitive).
     *
     * @param gender the gender to filter users by
     * @param city the city to filter users by (case-insensitive)
     * @return a list of users matching the specified gender and city
     */
    List<User> findByGenderAndAddress_CityIgnoreCase(Gender gender, String city);

    /**
     * Acquires a pessimistic write lock on the {@link User} entity with the specified ID.
     * <p>
     * This method executes a query to select the user by ID and applies a {@code PESSIMISTIC_WRITE} lock,
     * ensuring that other transactions cannot acquire a lock on the same entity for writing until the current
     * transaction is completed. The lock timeout is set to 5000 milliseconds.
     * </p>
     *
     * @param id the ID of the user to lock for update
     * @return the locked {@link User} entity
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    User lockForUpdate(@Param("id") Long id);
}