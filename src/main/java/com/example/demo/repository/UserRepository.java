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

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByNameAndFirstName(String name, String firstName);

    List<User> findByGenderAndAddress_CityIgnoreCase(Gender gender, String city);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    User lockForUpdate(@Param("id") Long id);
}