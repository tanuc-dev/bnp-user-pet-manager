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
