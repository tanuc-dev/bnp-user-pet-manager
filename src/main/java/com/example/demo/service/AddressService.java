package com.example.demo.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AddressCreateDto;
import com.example.demo.model.Address;
import com.example.demo.repository.AddressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository repo;

    private static String norm(String s) {
        if (s == null)
            return null;
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

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
