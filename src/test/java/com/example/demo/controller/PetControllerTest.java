package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.Address;
import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.service.AddressService;
import com.example.demo.service.PetService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PetController.class)
class PetControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PetService petService;
    @MockitoBean private AddressService addressService;

    private Address addr(long id) {
        Address a = new Address();
        a.setId(id);
        a.setCity("paris");
        a.setType("road");
        a.setAddressName("antoine lavoisier");
        a.setNumber("10");
        return a;
    }

    private Pet pet(long id) {
        Pet p = new Pet();
        p.setId(id);
        p.setName("Buddy");
        p.setAge(5);
        p.setType(PetType.DOG);
        p.setAddress(addr(1L));
        p.setDeceased(false);
        return p;
    }

    @Test
    void create_returnsPetDto() throws Exception {
        // Arrange
        given(addressService.findOrCreate(any())).willReturn(addr(1L));
        given(petService.save(any(Pet.class))).willAnswer(inv -> {
            Pet p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        Map<String, Object> body = Map.of(
            "name", "Buddy",
            "age", 5,
            "type", "DOG",
            "address", Map.of(
                "city", "Paris",
                "type", "Road",
                "addressName", "Antoine Lavoisier",
                "number", "10"
            )
        );

        // Act + Assert
        mvc.perform(post("/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(100))
           .andExpect(jsonPath("$.name").value("Buddy"))
           .andExpect(jsonPath("$.age").value(5))
           .andExpect(jsonPath("$.type").value("DOG"))
           .andExpect(jsonPath("$.deceased").value(false));

        then(addressService).should().findOrCreate(any());
        then(petService).should().save(any(Pet.class));
    }

    @Test
    void update_returnsUpdatedPetDto_andCallsAddressFindOrCreate() throws Exception {
        // Arrange address de-dup call made inside the lambda
        given(addressService.findOrCreate(any())).willReturn(addr(2L));

        // Make the service stub EXECUTE the Consumer so the controller's lambda runs
        given(petService.updateWithPessimisticLockAndRetry(eq(200L), any()))
            .willAnswer(inv -> {
                Consumer<Pet> mut = inv.getArgument(1);
                Pet base = pet(200L); // starting state before mutation
                mut.accept(base);     // triggers addressService.findOrCreate(...)
                return base;          // return mutated entity
            });

        Map<String, Object> body = Map.of(
            "name", "Nemo",
            "age", 2,
            "type", "OTHER",
            "address", Map.of(
                "city", "Paris",
                "type", "Road",
                "addressName", "Antoine Lavoisier",
                "number", "10"
            )
        );

        // Act + Assert
        mvc.perform(put("/pets/200")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(200))
           .andExpect(jsonPath("$.name").value("Nemo"))
           .andExpect(jsonPath("$.age").value(2))
           .andExpect(jsonPath("$.type").value("OTHER"));

        then(addressService).should().findOrCreate(any());
        then(petService).should().updateWithPessimisticLockAndRetry(eq(200L), any());
    }

    @Test
    void markDeceased_returnsDtoWithDeceasedTrue() throws Exception {
        Pet p = pet(9L);
        p.setDeceased(true);
        given(petService.markDeceased(9L)).willReturn(p);

        mvc.perform(patch("/pets/9/death"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(9))
           .andExpect(jsonPath("$.deceased").value(true));

        then(petService).should().markDeceased(9L);
    }

    @Test
    void create_validationFailure_missingName_returns400() throws Exception {
        Map<String, Object> badBody = Map.of(
            // "name" missing
            "age", 5,
            "type", "DOG",
            "address", Map.of(
                "city", "Paris",
                "type", "Road",
                "addressName", "Antoine Lavoisier",
                "number", "10"
            )
        );

        mvc.perform(post("/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badBody)))
           .andExpect(status().isBadRequest());
    }
}
