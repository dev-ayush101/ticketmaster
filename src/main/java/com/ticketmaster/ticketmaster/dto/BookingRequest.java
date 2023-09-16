package com.ticketmaster.ticketmaster.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingRequest {
    private List<UUID> ticketIds;
    private String userEmail;
}