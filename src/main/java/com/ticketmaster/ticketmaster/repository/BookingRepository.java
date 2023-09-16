package com.ticketmaster.ticketmaster.repository;

import com.ticketmaster.ticketmaster.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
}