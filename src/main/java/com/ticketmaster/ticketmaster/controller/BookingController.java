package com.ticketmaster.ticketmaster.controller;

import com.ticketmaster.ticketmaster.dto.BookingRequest;
import com.ticketmaster.ticketmaster.model.Booking;
import com.ticketmaster.ticketmaster.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/{eventId}")
    public ResponseEntity<Booking> reserve(@PathVariable UUID eventId, @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.reserveTickets(eventId, request));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId));
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Booking> confirm(@PathVariable UUID bookingId, @RequestParam String userEmail) {
        return ResponseEntity.ok(bookingService.confirmBooking(bookingId, userEmail));
    }
}
