package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.model.Address;
import com.example.demo.model.Gender;
import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;
import com.example.demo.service.PetService;
import com.example.demo.service.UserPetOwnershipService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(OwnershipController.class)
@Import(GlobalExceptionHandler.class) // so IllegalArgumentException -> 400
class OwnershipControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private PetService petService;
    @MockitoBean
    private UserPetOwnershipService ownershipService;

    // ---- helpers ----
    private Address addr(long id, String city) {
        Address a = new Address();
        a.setId(id);
        a.setCity(city);
        a.setType("road");
        a.setAddressName("antoine lavoisier");
        a.setNumber("10");
        return a;
    }

    private User user(long id, Address a, String name, String firstName, Gender g) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setFirstName(firstName);
        u.setGender(g);
        u.setAge(30);
        u.setAddress(a);
        u.setDeceased(false);
        return u;
    }

    private Pet pet(long id, Address a, String name, PetType type, boolean deceased) {
        Pet p = new Pet();
        p.setId(id);
        p.setName(name);
        p.setType(type);
        p.setAge(5);
        p.setAddress(a);
        p.setDeceased(deceased);
        return p;
    }

    private UserPetOwnership own(long id, User u, Pet p) {
        UserPetOwnership o = new UserPetOwnership();
        o.setId(id);
        o.setUser(u);
        o.setPet(p);
        return o;
    }

    @Test
    void link_success_sameAddress_returns200_andSaves() throws Exception {
        var a = addr(1L, "paris");
        var u = user(10L, a, "Doe", "John", Gender.MALE);
        var p = pet(20L, a, "Buddy", PetType.DOG, false);

        given(userService.getOrThrow(10L)).willReturn(u);
        given(petService.getOrThrow(20L)).willReturn(p);
        given(ownershipService.save(any(UserPetOwnership.class))).willAnswer(inv -> inv.getArgument(0));

        Map<String, Object> body = Map.of("userId", 10, "petId", 20);

        mvc.perform(post("/ownerships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        then(ownershipService).should()
                .save(argThat(o -> o.getUser().getId().equals(10L) && o.getPet().getId().equals(20L)));
    }

    @Test
    void link_mismatchAddress_returns400_andDoesNotSave() throws Exception {
        var au = addr(1L, "paris");
        var ap = addr(2L, "mumbai");
        var u = user(10L, au, "Doe", "John", Gender.MALE);
        var p = pet(20L, ap, "Buddy", PetType.DOG, false);

        given(userService.getOrThrow(10L)).willReturn(u);
        given(petService.getOrThrow(20L)).willReturn(p);

        Map<String, Object> body = Map.of("userId", 10, "petId", 20);

        mvc.perform(post("/ownerships")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest()) // GlobalExceptionHandler maps IllegalArgumentException -> 400
                .andExpect(jsonPath("$.message").value("Co-ownership allowed only for users at the pet's address."));

        then(ownershipService).should(never()).save(any());
    }

    @Test
    void petsByUser_handlesHomonyms_filtersDeceased_distinct() throws Exception {
        // two users same name/firstName
        var a = addr(1L, "paris");
        var u1 = user(1L, a, "Doe", "John", Gender.MALE);
        var u2 = user(2L, a, "Doe", "John", Gender.MALE);

        var petAlive = pet(100L, a, "Buddy", PetType.DOG, false);
        var petDeceased = pet(101L, a, "Shadow", PetType.CAT, true);

        // duplicate ownerships to test distinct
        var o1 = own(11L, u1, petAlive);
        var o2 = own(12L, u2, petAlive);
        var o3 = own(13L, u1, petDeceased);

        given(userService.byNameFirstName("Doe", "John")).willReturn(List.of(u1, u2));
        given(ownershipService.byUser(u1)).willReturn(List.of(o1, o3));
        given(ownershipService.byUser(u2)).willReturn(List.of(o2));

        mvc.perform(get("/ownerships/pets-by-user")
                .param("name", "Doe")
                .param("firstName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].name").value("Buddy"))
                .andExpect(jsonPath("$[0].type").value("DOG"))
                .andExpect(jsonPath("$[0].deceased").value(false));
    }

    @Test
    void petsByCity_delegatesToPetService_andMaps() throws Exception {
        var a = addr(1L, "paris");
        var p1 = pet(1L, a, "Buddy", PetType.DOG, false);
        var p2 = pet(2L, a, "Pixie", PetType.CAT, false);
        given(petService.byCity("paris")).willReturn(List.of(p1, p2));

        mvc.perform(get("/ownerships/pets-by-city").param("city", "paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Pixie"));
    }

    @Test
    void usersByPetTypeAndCity_returnsUserDtos_withAddress() throws Exception {
        var a = addr(1L, "paris");
        var u1 = user(10L, a, "Doe", "Jane", Gender.FEMALE);
        var u2 = user(11L, a, "Smith", "Anna", Gender.FEMALE);

        given(ownershipService.usersByPetTypeAndCity(PetType.DOG, "paris")).willReturn(List.of(u1, u2));

        mvc.perform(get("/ownerships/users-by-pet-type-and-city")
                .param("petType", "DOG")
                .param("city", "paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].address.city").value("paris"))
                .andExpect(jsonPath("$[1].firstName").value("Anna"));
    }

    @Test
    void petsByWomenInCity_filtersDeceased_distinct() throws Exception {
        var a = addr(1L, "mumbai");
        var woman1 = user(100L, a, "Patel", "Priya", Gender.FEMALE);
        var woman2 = user(101L, a, "Khan", "Sara", Gender.FEMALE);

        var petAlive = pet(7L, a, "Bruno", PetType.DOG, false);
        var petDeceased = pet(8L, a, "Rocky", PetType.DOG, true);

        var o1 = own(31L, woman1, petAlive);
        var o2 = own(32L, woman1, petDeceased);
        var o3 = own(33L, woman2, petAlive); // duplicate pet → expect distinct

        given(userService.womenInCity("mumbai")).willReturn(List.of(woman1, woman2));
        given(ownershipService.byUser(woman1)).willReturn(List.of(o1, o2));
        given(ownershipService.byUser(woman2)).willReturn(List.of(o3));

        mvc.perform(get("/ownerships/pets-by-women-in-city").param("city", "mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Bruno"))
                .andExpect(jsonPath("$[0].deceased").value(false));
    }
}
