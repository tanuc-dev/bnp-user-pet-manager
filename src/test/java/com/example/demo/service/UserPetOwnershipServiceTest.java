package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.model.User;
import com.example.demo.model.UserPetOwnership;
import com.example.demo.repository.UserPetOwnershipRepository;

@ExtendWith(SpringExtension.class)
@Import(UserPetOwnershipService.class)
class UserPetOwnershipServiceTest {

    @MockitoBean
    private UserPetOwnershipRepository repo;

    @Autowired
    private UserPetOwnershipService service;

    @Test
    void save_delegatesToRepository() {
        var u = User.builder().id(1L).build();
        var p = Pet.builder().id(2L).build();
        var ownership = UserPetOwnership.builder().id(10L).user(u).pet(p).build();

        given(repo.save(any(UserPetOwnership.class))).willReturn(ownership);

        var result = service.save(UserPetOwnership.builder().user(u).pet(p).build());

        assertThat(result).isSameAs(ownership);
        then(repo).should().save(argThat(o -> o.getUser() == u && o.getPet() == p));
    }

    @Test
    void byUser_returnsListFromRepo() {
        var u = User.builder().id(1L).build();
        var o1 = UserPetOwnership.builder().id(11L).user(u).pet(Pet.builder().id(2L).build()).build();
        var o2 = UserPetOwnership.builder().id(12L).user(u).pet(Pet.builder().id(3L).build()).build();

        given(repo.findByUser(u)).willReturn(List.of(o1, o2));

        var result = service.byUser(u);

        assertThat(result).containsExactly(o1, o2);
        then(repo).should().findByUser(u);
    }

    @Test
    void byPet_returnsListFromRepo() {
        var p = Pet.builder().id(2L).build();
        var o1 = UserPetOwnership.builder().id(21L).user(User.builder().id(1L).build()).pet(p).build();
        var o2 = UserPetOwnership.builder().id(22L).user(User.builder().id(2L).build()).pet(p).build();

        given(repo.findByPet(p)).willReturn(List.of(o1, o2));

        var result = service.byPet(p);

        assertThat(result).containsExactly(o1, o2);
        then(repo).should().findByPet(p);
    }

    @Test
    void usersByPetTypeAndCity_returnsDistinctUsersFromRepo() {
        var u1 = User.builder().id(1L).build();
        var u2 = User.builder().id(2L).build();

        given(repo.findDistinctUsersByPetTypeAndCity(PetType.DOG, "paris"))
                .willReturn(List.of(u1, u2));

        var result = service.usersByPetTypeAndCity(PetType.DOG, "paris");

        assertThat(result).containsExactly(u1, u2);
        then(repo).should().findDistinctUsersByPetTypeAndCity(PetType.DOG, "paris");
    }
}
