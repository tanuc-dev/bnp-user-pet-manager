package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.example.demo.model.Pet;
import com.example.demo.model.PetType;
import com.example.demo.repository.PetRepository;

@ExtendWith(SpringExtension.class)
@Import({PetService.class, PetServiceTest.Config.class})
class PetServiceTest {

    @TestConfiguration
    @EnableRetry
    @EnableTransactionManagement
    static class Config {
        // Minimal TM so @Transactional works without a real DataSource
        @Bean
        PlatformTransactionManager txManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object();}
                @Override protected void doBegin(Object tx, TransactionDefinition def) {}
                @Override protected void doCommit(DefaultTransactionStatus status) throws TransactionException {}
                @Override protected void doRollback(DefaultTransactionStatus status) throws TransactionException {}
            };
        }
    }

    @MockitoBean
    private PetRepository repo;

    @Autowired
    private PetService service;

    @Test
    void save_delegatesToRepository() {
        Pet p = Pet.builder().id(1L).name("Buddy").build();
        given(repo.save(any(Pet.class))).willReturn(p);

        Pet saved = service.save(Pet.builder().name("Buddy").build());

        assertThat(saved).isSameAs(p);
        then(repo).should().save(argThat(x -> "Buddy".equals(x.getName())));
    }

    @Test
    void getOrThrow_returnsEntity_whenPresent() {
        Pet p = Pet.builder().id(10L).name("Kitty").build();
        given(repo.findById(10L)).willReturn(Optional.of(p));

        Pet found = service.getOrThrow(10L);

        assertThat(found).isSameAs(p);
        then(repo).should().findById(10L);
    }

    @Test
    void getOrThrow_throws_whenMissing() {
        given(repo.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrThrow(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pet not found: 99");
    }

    @Test
    void byType_delegatesToRepository() {
        List<Pet> expected = List.of(Pet.builder().id(1L).type(PetType.DOG).build());
        given(repo.findByType(PetType.DOG)).willReturn(expected);

        List<Pet> result = service.byType(PetType.DOG);

        assertThat(result).isSameAs(expected);
        then(repo).should().findByType(PetType.DOG);
    }

    @Test
    void byCity_delegatesToRepository() {
        List<Pet> expected = List.of(Pet.builder().id(2L).name("Buddy").build());
        given(repo.findByAddress_CityIgnoreCaseAndDeceasedFalse("paris")).willReturn(expected);

        List<Pet> result = service.byCity("paris");

        assertThat(result).isSameAs(expected);
        then(repo).should().findByAddress_CityIgnoreCaseAndDeceasedFalse("paris");
    }

    @Test
    void updateWithPessimisticLockAndRetry_success_noRetry() {
        Pet locked = Pet.builder().id(5L).name("Nemo").age(1).build();
        given(repo.lockForUpdate(5L)).willReturn(locked);
        // saveAndFlush returns the mutated entity
        given(repo.saveAndFlush(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

        Pet updated = service.updateWithPessimisticLockAndRetry(5L, p -> p.setAge(p.getAge() + 1));

        assertThat(updated.getAge()).isEqualTo(2);
        then(repo).should(times(1)).lockForUpdate(5L);
        then(repo).should().saveAndFlush(locked);
    }

    @Test
    void updateWithPessimisticLockAndRetry_retriesOnTransientLock_thenSucceeds() {
        Pet locked = Pet.builder().id(6L).name("Coco").age(3).build();

        AtomicInteger calls = new AtomicInteger();
        // First two attempts throw CannotAcquireLockException, third returns entity
        given(repo.lockForUpdate(6L)).willAnswer(inv -> {
            switch (calls.getAndIncrement()) {
                case 0, 1 -> throw new org.springframework.dao.CannotAcquireLockException("busy");
                default -> { return locked; }
            }
        });
        given(repo.saveAndFlush(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

        Pet updated = service.updateWithPessimisticLockAndRetry(6L, p -> p.setAge(p.getAge() + 5));

        assertThat(updated.getAge()).isEqualTo(8);
        then(repo).should(times(3)).lockForUpdate(6L);
        then(repo).should(times(1)).saveAndFlush(locked);
    }

    @Test
    void updateWithPessimisticLockAndRetry_throwsWhenNotFound() {
        // lockForUpdate returns null -> triggers RuntimeException("Pet not found: id")
        given(repo.lockForUpdate(404L)).willReturn(null);

        assertThatThrownBy(() -> service.updateWithPessimisticLockAndRetry(404L, p -> {}))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Pet not found: 404");
        then(repo).should(times(1)).lockForUpdate(404L);
        then(repo).should(never()).saveAndFlush(any());
    }

    @Test
    void markDeceased_setsFlagAndSaves() {
        Pet p = Pet.builder().id(9L).name("Tiger").deceased(false).build();
        given(repo.findById(9L)).willReturn(Optional.of(p));
        given(repo.save(any(Pet.class))).willAnswer(inv -> inv.getArgument(0));

        Pet result = service.markDeceased(9L);

        assertThat(result.isDeceased()).isTrue();
        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        then(repo).should().save(captor.capture());
        assertThat(captor.getValue().isDeceased()).isTrue();
    }
}

