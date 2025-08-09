package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.repository.PetRepository;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;

/**
 * Service class for managing Pet entities.
 * <p>
 * Provides methods for saving, retrieving, updating, and querying pets,
 * including support for pessimistic locking and retry mechanisms for concurrent updates.
 * </p>
 *
 * <ul>
 *   <li>{@link #save(Pet)} - Persists a new or existing Pet entity.</li>
 *   <li>{@link #getOrThrow(Long)} - Retrieves a Pet by ID or throws an exception if not found.</li>
 *   <li>{@link #byType(PetType)} - Finds pets by their type.</li>
 *   <li>{@link #byCity(String)} - Finds pets by city, excluding deceased pets.</li>
 *   <li>{@link #updateWithPessimisticLockAndRetry(Long, Consumer)} - Updates a Pet with pessimistic locking and retry logic for concurrency control.</li>
 *   <li>{@link #markDeceased(Long)} - Marks a Pet as deceased.</li>
 * </ul>
 *
 * <p>
 * This service relies on {@link PetRepository} for data access and uses Spring's
 * {@code @Transactional} and {@code @Retryable} annotations for transaction and retry management.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PetService {
    private final PetRepository repo;

    public Pet save(Pet p) {
        return repo.save(p);
    }

    public Pet getOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Pet not found: " + id));
    }

    public List<Pet> byType(PetType type) {
        return repo.findByType(type);
    }

    public List<Pet> byCity(String city) {
        return repo.findByAddress_CityIgnoreCaseAndDeceasedFalse(city);
    }

    @Retryable(retryFor = { 
            PessimisticLockException.class, 
            LockTimeoutException.class,
            CannotAcquireLockException.class }, 
            maxAttempts = 3, 
            backoff = @Backoff(delay = 50, multiplier = 2.0))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pet updateWithPessimisticLockAndRetry(Long id, Consumer<Pet> mutator) {
        Pet u = Optional.ofNullable(repo.lockForUpdate(id)) // acquires PESSIMISTIC_WRITE
                .orElseThrow(() -> new RuntimeException("Pet not found: " + id));
        mutator.accept(u);
        return repo.saveAndFlush(u);
    }
   
    public Pet markDeceased(Long id) {
        Pet p = getOrThrow(id);
        p.setDeceased(true);
        return repo.save(p);
    }
}
