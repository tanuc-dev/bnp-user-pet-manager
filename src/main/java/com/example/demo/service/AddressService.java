package com.example.demo.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AddressCreateDto;
import com.example.demo.model.Address;
import com.example.demo.repository.AddressRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class for managing Address entities.
 * <p>
 * Provides functionality to normalize address fields, find existing addresses,
 * or create new ones if they do not exist. Handles concurrent insertions gracefully.
 * </p>
 *
 * <p>
 * Main methods:
 * <ul>
 *   <li>{@link #findOrCreate(AddressCreateDto)} - Finds an address by normalized fields or creates a new one.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Dependencies:
 * <ul>
 *   <li>{@link AddressRepository} - Repository for Address persistence operations.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Thread Safety:
 * <ul>
 *   <li>Handles concurrent insertions using exception handling and re-querying.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository repo;

    private static String norm(String s) {
        if (s == null)
            return null;
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /**
     * Finds an existing {@link Address} entity matching the given parameters, or creates a new one if none exists.
     * <p>
     * The method normalizes the input fields from the {@link AddressCreateDto} and attempts to find an address
     * in the repository using a case-insensitive search on city, type, address name, and number.
     * If an address is found, it is returned. Otherwise, a new address is created and saved.
     * <p>
     * Handles potential race conditions where another thread/request may insert the same address concurrently
     * by catching {@link DataIntegrityViolationException} and re-querying for the address.
     *
     * @param dto the data transfer object containing address details
     * @return the existing or newly created {@link Address}
     * @throws DataIntegrityViolationException if a concurrent insert fails and the address cannot be found afterwards
     */
    @Transactional
    public Address findOrCreate(AddressCreateDto dto) {
        String city = norm(dto.city());
        String type = norm(dto.type());
        String name = norm(dto.addressName());
        String number = norm(dto.number());

        Optional<Address> found = repo
            .findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(city, type, name, number);

        if (found.isPresent())
            return found.get();

        Address toSave = Address.builder()
                .city(city)
                .type(type)
                .addressName(name)
                .number(number)
                .build();

        try {
            return repo.save(toSave);
        } catch (DataIntegrityViolationException e) {
            // Another thread/request inserted same address concurrently
            return repo
                .findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(city, type, name, number)
                    .orElseThrow(() -> e);
        }
    }
}
