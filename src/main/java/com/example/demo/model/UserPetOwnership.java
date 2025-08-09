package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_pet_ownership", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "pet_id" }))
public class UserPetOwnership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;
    @ManyToOne(optional = false)
    private Pet pet;
}
