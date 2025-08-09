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

import com.example.demo.model.Gender;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;

/**
 * Service class for managing User entities.
 * <p>
 * Provides methods for saving, retrieving, updating, and querying users,
 * including support for pessimistic locking and retry mechanisms for concurrent updates.
 * </p>
 *
 * <ul>
 *   <li>{@link #save(User)} - Persists a new or existing user.</li>
 *   <li>{@link #getOrThrow(Long)} - Retrieves a user by ID or throws an exception if not found.</li>
 *   <li>{@link #byNameFirstName(String, String)} - Finds users by name and first name.</li>
 *   <li>{@link #womenInCity(String)} - Finds female users in a specified city.</li>
 *   <li>{@link #updateWithPessimisticLockAndRetry(Long, Consumer)} - Updates a user with pessimistic locking and retry logic.</li>
 *   <li>{@link #markDeceased(Long)} - Marks a user as deceased.</li>
 * </ul>
 *
 * <p>
 * This service uses Spring's {@code @Transactional} and {@code @Retryable} annotations
 * to ensure data consistency and handle transient locking issues.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;

    public User save(User u) {
        return repo.save(u);
    }

    public User getOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public List<User> byNameFirstName(String name, String firstName) {
        return repo.findByNameAndFirstName(name, firstName);
    }

    public List<User> womenInCity(String city) {
        return repo.findByGenderAndAddress_CityIgnoreCase(Gender.FEMALE, city);
    }

    @Retryable(
        retryFor = {
            PessimisticLockException.class,
            LockTimeoutException.class,
            CannotAcquireLockException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0) // 50ms, then 100ms
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User updateWithPessimisticLockAndRetry(Long id, Consumer<User> mutator) {
        User u = Optional.ofNullable(repo.lockForUpdate(id)) // acquires PESSIMISTIC_WRITE
        .orElseThrow(() -> new RuntimeException("User not found: " + id));
        mutator.accept(u);
        return repo.saveAndFlush(u);     // flush inside the same tx
    }

    public User markDeceased(Long id) {
        User u = getOrThrow(id);
        u.setDeceased(true);
        return repo.save(u);
    }
}
