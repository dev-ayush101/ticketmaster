package com.ticketmaster.ticketmaster.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    private String section;
    private String rowName;
    private int seatNumber;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private LocalDateTime reservedUntil;
    private LocalDateTime createdAt;
}