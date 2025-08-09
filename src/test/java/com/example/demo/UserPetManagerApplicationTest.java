package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(classes = UserPetManagerApplication.class)
class UserPetManagerApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Verifies the Spring context starts successfully
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void mainMethod_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(eq(UserPetManagerApplication.class), any(String[].class)))
                    .thenReturn(Mockito.mock(ConfigurableApplicationContext.class));

            UserPetManagerApplication.main(new String[] {});

            mocked.verify(() -> SpringApplication.run(eq(UserPetManagerApplication.class), any(String[].class)));
        }
    }
}
