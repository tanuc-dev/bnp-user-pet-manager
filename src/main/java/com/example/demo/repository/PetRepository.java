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

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByType(PetType type);

    List<Pet> findByAddress_CityIgnoreCaseAndDeceasedFalse(String city);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id")
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") })
    Pet lockForUpdate(@Param("id") Long id);
}
