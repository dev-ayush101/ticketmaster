package com.ticketmaster.ticketmaster.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "performers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Performer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
}