package com.ticketmaster.ticketmaster.dto;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatUpdate {
    private UUID ticketId;
    private String status; // AVAILABLE, RESERVED, BOOKED
}