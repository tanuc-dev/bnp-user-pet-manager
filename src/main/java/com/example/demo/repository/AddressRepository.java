package com.example.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Address;

/**
 * Repository interface for managing {@link Address} entities.
 * <p>
 * Provides methods to perform CRUD operations and custom queries on Address data.
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
 *     {@code findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase}:
 *     Retrieves an {@link Address} entity matching the specified city, type, address name, and number,
 *     ignoring case sensitivity for all fields.
 *   </li>
 * </ul>
 * </p>
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Retrieves an {@link Optional} {@link Address} entity that matches the specified city, type, address name, and number,
     * ignoring case sensitivity for all parameters.
     *
     * @param city the city to search for (case-insensitive)
     * @param type the type of address to search for (case-insensitive)
     * @param addressName the name of the address to search for (case-insensitive)
     * @param number the number of the address to search for (case-insensitive)
     * @return an {@link Optional} containing the matching {@link Address} if found, or empty if no match exists
     */
    Optional<Address> findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
            String city, String type, String addressName, String number);
}
