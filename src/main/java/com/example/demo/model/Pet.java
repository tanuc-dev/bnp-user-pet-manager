package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    private Integer age;

    @Enumerated(EnumType.STRING)
    private PetType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "address_id")
    private Address address;

    @Builder.Default
    @Column(name = "is_deceased")
    private boolean deceased = false;
}
