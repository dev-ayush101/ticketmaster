package com.ticketmaster.ticketmaster.repository;

import com.ticketmaster.ticketmaster.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByEventId(UUID eventId);
}
