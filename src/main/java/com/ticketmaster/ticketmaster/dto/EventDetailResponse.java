package com.ticketmaster.ticketmaster.dto;

import com.ticketmaster.ticketmaster.model.Event;
import com.ticketmaster.ticketmaster.model.Ticket;
import lombok.*;

import java.util.List;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class EventDetailResponse {
    private Event event;
    private List<Ticket> tickets;
}