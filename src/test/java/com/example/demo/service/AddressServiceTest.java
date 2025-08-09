package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.demo.dto.AddressCreateDto;
import com.example.demo.model.Address;
import com.example.demo.repository.AddressRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository repo;

    private AddressService service;

    @BeforeEach
    void setUp() {
        service = new AddressService(repo);
    }

    private AddressCreateDto dto(String city, String type, String name, String number) {
        return new AddressCreateDto(city, type, name, number);
    }

    @Test
    void findOrCreate_returnsExisting_whenRepoFindsAddress() {
        // Arrange
        Address existing = Address.builder()
                .id(42L).city("paris").type("road").addressName("antoine lavoisier").number("10")
                .build();

        when(repo.findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        // Act
        Address result = service.findOrCreate(dto("Paris", "Road", "Antoine   Lavoisier", "10"));

        // Assert
        assertThat(result).isSameAs(existing);
        verify(repo, times(1))
                .findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                        "paris", "road", "antoine lavoisier", "10");
        verify(repo, never()).save(any());
    }

    @Test
    void findOrCreate_savesAndReturns_whenNotFound() {
        // Arrange
        when(repo.findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        Address saved = Address.builder()
                .id(7L).city("paris").type("road").addressName("antoine lavoisier").number("10")
                .build();

        when(repo.save(any(Address.class))).thenReturn(saved);

        // Act
        Address result = service.findOrCreate(dto("Paris", "Road", "Antoine Lavoisier", "10"));

        // Assert
        assertThat(result.getId()).isEqualTo(7L);
        verify(repo).save(argThat(a -> a.getCity().equals("paris")
                && a.getType().equals("road")
                && a.getAddressName().equals("antoine lavoisier")
                && a.getNumber().equals("10")));
    }

    @Test
    void findOrCreate_handlesRace_onUniqueConstraint() {
        // Arrange: first find -> empty, save -> throws, second find -> present
        when(repo.findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty()) // initial lookup
                .thenReturn(Optional.of(Address.builder() // re-fetch after race
                        .id(99L).city("paris").type("road")
                        .addressName("antoine lavoisier").number("10")
                        .build()));

        when(repo.save(any(Address.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // Act
        Address result = service.findOrCreate(dto("PARIS", "ROAD", "Antoine  Lavoisier", "10"));

        // Assert
        assertThat(result.getId()).isEqualTo(99L);
        verify(repo, times(2))
                .findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                        "paris", "road", "antoine lavoisier", "10");
        verify(repo, times(1)).save(any(Address.class));
    }

    @Test
    void findOrCreate_normalizesInput_trimCollapseSpacesAndLowercase() {
        // Arrange
        ArgumentCaptor<String> cityCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> numCap = ArgumentCaptor.forClass(String.class);

        when(repo.findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                cityCap.capture(), typeCap.capture(), nameCap.capture(), numCap.capture()))
                .thenReturn(Optional.of(Address.builder()
                        .id(1L).city("paris").type("road").addressName("antoine lavoisier").number("10").build()));

        // Act
        service.findOrCreate(dto("  PaRiS ", "  RoAd ", "  Antoine   Lavoisier  ", "  10   "));

        // Assert: normalized to lower + single spaces + trimmed
        assertThat(cityCap.getValue()).isEqualTo("paris");
        assertThat(typeCap.getValue()).isEqualTo("road");
        assertThat(nameCap.getValue()).isEqualTo("antoine lavoisier");
        assertThat(numCap.getValue()).isEqualTo("10");
    }

    @Test
    void findOrCreate_throwsWhenSecondFindEmptyAfterRace() {
        // Arrange: first find -> empty, save -> throws, second find -> empty
        when(repo.findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                any(), any(), any(), any()))
                .thenReturn(Optional.empty()) // first lookup
                .thenReturn(Optional.empty()); // re-fetch after race

        when(repo.save(any(Address.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        AddressCreateDto dto = new AddressCreateDto(
                null, // city is null to test norm() returning null
                "ROAD",
                "Antoine Lavoisier",
                "10");

        // Act + Assert
        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> service.findOrCreate(dto));

        // Verify: both lookups were done with normalized args (null for city)
        verify(repo, times(2))
                .findByCityIgnoreCaseAndTypeIgnoreCaseAndAddressNameIgnoreCaseAndNumberIgnoreCase(
                        isNull(), eq("road"), eq("antoine lavoisier"), eq("10"));
        verify(repo, times(1)).save(any(Address.class));
    }
}
