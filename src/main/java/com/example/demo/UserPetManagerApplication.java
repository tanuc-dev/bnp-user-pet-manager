package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * The main entry point for the User Pet Manager Spring Boot application.
 * <p>
 * This class is annotated with {@link org.springframework.retry.annotation.EnableRetry}
 * to enable support for retryable operations, and {@link org.springframework.boot.autoconfigure.SpringBootApplication}
 * to mark it as a Spring Boot application.
 * </p>
 * <p>
 * The {@code main} method starts the application using {@link org.springframework.boot.SpringApplication}.
 * </p>
 */
@EnableRetry
@SpringBootApplication
public class UserPetManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserPetManagerApplication.class, args);
	}

}
