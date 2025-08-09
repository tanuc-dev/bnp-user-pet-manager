package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "address", uniqueConstraints = @UniqueConstraint(columnNames = { "city", "type", "address_name", "number" }))
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String type; // road, street, avenue
    @Column(name = "address_name", nullable = false)
    private String addressName;
    @Column(nullable = false)
    private String number; // "10" etc.

}
