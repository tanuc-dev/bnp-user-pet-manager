package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.Address;
import com.example.demo.model.Gender;
import com.example.demo.model.User;
import com.example.demo.service.AddressService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web layer test for UserController (no DB, services are mocked).
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AddressService addressService;

    private Address addr(Long id) {
        Address a = new Address();
        a.setId(id);
        a.setCity("paris");
        a.setType("road");
        a.setAddressName("antoine lavoisier");
        a.setNumber("10");
        return a;
    }

    private User user(Long id, Address a) {
        User u = new User();
        u.setId(id);
        u.setName("Doe");
        u.setFirstName("John");
        u.setAge(30);
        u.setGender(Gender.MALE);
        u.setAddress(a);
        u.setDeceased(false);
        return u;
    }

    @Test
    void create_returnsUserDto() throws Exception {
        // Given
        Address savedAddr = addr(1L);
        given(addressService.findOrCreate(any())).willReturn(savedAddr);
        given(userService.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        // Request body (matches UserCreateDto shape)
        Map<String, Object> body = Map.of(
                "name", "Doe",
                "firstName", "John",
                "age", 30,
                "gender", "MALE",
                "address", Map.of(
                        "city", "Paris",
                        "type", "Road",
                        "addressName", "Antoine Lavoisier",
                        "number", "10"));

        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Doe"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.address.id").value(1))
                .andExpect(jsonPath("$.address.city").value("paris"))
                .andExpect(jsonPath("$.address.type").value("road"))
                .andExpect(jsonPath("$.address.addressName").value("antoine lavoisier"))
                .andExpect(jsonPath("$.address.number").value("10"));

        then(addressService).should().findOrCreate(any());
        then(userService).should().save(any(User.class));
    }

    @Test
    void update_returnsUpdatedUserDto() throws Exception {
        // Address returned by addressService when controller calls it inside the lambda
        Address newAddr = addr(2L);
        given(addressService.findOrCreate(any())).willReturn(newAddr);

        // Make the service stub execute the Consumer<User> passed by the controller
        given(userService.updateWithPessimisticLockAndRetry(eq(200L), any()))
                .willAnswer(inv -> {
                    Consumer<User> mut = inv.getArgument(1);
                    User base = user(200L, addr(1L)); // starting state before mutation
                    mut.accept(base); // <-- executes addressService.findOrCreate(...)
                    return base; // service would return the mutated entity
                });

        Map<String, Object> body = Map.of(
                "name", "Doe",
                "firstName", "Jane",
                "age", 25,
                "gender", "FEMALE",
                "address", Map.of(
                        "city", "Paris",
                        "type", "Road",
                        "addressName", "Antoine Lavoisier",
                        "number", "10"));

        mvc.perform(put("/users/200")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.address.id").value(2)); // moved to newAddr

        // Now this verify will pass because the lambda actually ran
        then(addressService).should().findOrCreate(any());
        then(userService).should().updateWithPessimisticLockAndRetry(eq(200L), any());
    }

    @Test
    void markDeceased_returnsDtoWithDeceasedTrue() throws Exception {
        User u = user(5L, addr(1L));
        u.setDeceased(true);
        given(userService.markDeceased(5L)).willReturn(u);

        mvc.perform(patch("/users/5/death"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.deceased").value(true));

        then(userService).should().markDeceased(5L);
    }

    @Test
    void byName_returnsListOfUserDtos() throws Exception {
        User u1 = user(10L, addr(1L));
        User u2 = user(11L, addr(1L));
        u2.setFirstName("Johnny");

        given(userService.byNameFirstName("Doe", "John")).willReturn(List.of(u1, u2));

        mvc.perform(get("/users/by-name")
                .param("name", "Doe")
                .param("firstName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].firstName").value("Johnny"));

        then(userService).should().byNameFirstName("Doe", "John");
    }
}
