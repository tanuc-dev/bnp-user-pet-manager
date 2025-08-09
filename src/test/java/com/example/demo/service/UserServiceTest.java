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

import com.example.demo.model.Gender;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@ExtendWith(SpringExtension.class)
@Import({UserService.class, UserServiceTest.Config.class})
class UserServiceTest {

  /** Minimal TX infra: no DataSource/H2; satisfies @Transactional(REQUIRES_NEW). */
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
  private UserRepository repo;

  @Autowired
  private UserService service;

  @Test
  void save_delegatesToRepository() {
    var u = User.builder().id(1L).name("Doe").firstName("John").build();
    given(repo.save(any(User.class))).willReturn(u);

    var saved = service.save(User.builder().name("Doe").firstName("John").build());

    assertThat(saved).isSameAs(u);
    then(repo).should().save(argThat(x -> "Doe".equals(x.getName()) && "John".equals(x.getFirstName())));
  }

  @Test
  void getOrThrow_returnsUser_whenPresent() {
    var u = User.builder().id(10L).name("A").firstName("B").build();
    given(repo.findById(10L)).willReturn(Optional.of(u));

    assertThat(service.getOrThrow(10L)).isSameAs(u);
    then(repo).should().findById(10L);
  }

  @Test
  void getOrThrow_throws_whenMissing() {
    given(repo.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.getOrThrow(99L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("User not found: 99");
  }

  @Test
  void byNameFirstName_delegates() {
    var list = List.of(User.builder().id(1L).name("Doe").firstName("Jane").build());
    given(repo.findByNameAndFirstName("Doe", "Jane")).willReturn(list);

    assertThat(service.byNameFirstName("Doe", "Jane")).isSameAs(list);
    then(repo).should().findByNameAndFirstName("Doe", "Jane");
  }

  @Test
  void womenInCity_delegates() {
    var list = List.of(User.builder().id(2L).gender(Gender.FEMALE).build());
    given(repo.findByGenderAndAddress_CityIgnoreCase(Gender.FEMALE, "paris")).willReturn(list);

    assertThat(service.womenInCity("paris")).isSameAs(list);
    then(repo).should().findByGenderAndAddress_CityIgnoreCase(Gender.FEMALE, "paris");
  }

  @Test
  void updateWithPessimisticLockAndRetry_success_noRetry() {
    var locked = User.builder().id(5L).age(30).build();
    given(repo.lockForUpdate(5L)).willReturn(locked);
    given(repo.saveAndFlush(any(User.class))).willAnswer(inv -> inv.getArgument(0));

    var updated = service.updateWithPessimisticLockAndRetry(5L, u -> u.setAge(u.getAge() + 1));

    assertThat(updated.getAge()).isEqualTo(31);
    then(repo).should(times(1)).lockForUpdate(5L);
    then(repo).should().saveAndFlush(locked);
  }

  @Test
  void updateWithPessimisticLockAndRetry_retriesOnTransientLock_thenSucceeds() {
    var locked = User.builder().id(6L).age(20).build();
    var calls = new AtomicInteger();

    given(repo.lockForUpdate(6L)).willAnswer(inv -> {
      switch (calls.getAndIncrement()) {
        case 0, 1 -> throw new org.springframework.dao.CannotAcquireLockException("busy");
        default -> { return locked; }
      }
    });
    given(repo.saveAndFlush(any(User.class))).willAnswer(inv -> inv.getArgument(0));

    var updated = service.updateWithPessimisticLockAndRetry(6L, u -> u.setAge(u.getAge() + 5));

    assertThat(updated.getAge()).isEqualTo(25);
    then(repo).should(times(3)).lockForUpdate(6L);
    then(repo).should(times(1)).saveAndFlush(locked);
  }

  @Test
  void updateWithPessimisticLockAndRetry_throwsWhenNotFound() {
    given(repo.lockForUpdate(404L)).willReturn(null);

    assertThatThrownBy(() -> service.updateWithPessimisticLockAndRetry(404L, u -> {}))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("User not found: 404");
    then(repo).should(times(1)).lockForUpdate(404L);
    then(repo).should(never()).saveAndFlush(any());
  }

  @Test
  void markDeceased_setsFlagAndSaves() {
    var u = User.builder().id(9L).deceased(false).build();
    given(repo.findById(9L)).willReturn(Optional.of(u));
    given(repo.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

    var result = service.markDeceased(9L);

    assertThat(result.isDeceased()).isTrue();
    var cap = ArgumentCaptor.forClass(User.class);
    then(repo).should().save(cap.capture());
    assertThat(cap.getValue().isDeceased()).isTrue();
  }
}

