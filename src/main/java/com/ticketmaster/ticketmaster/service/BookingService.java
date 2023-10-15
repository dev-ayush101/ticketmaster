package com.ticketmaster.ticketmaster.service;

import com.ticketmaster.ticketmaster.dto.BookingRequest;
import com.ticketmaster.ticketmaster.dto.SeatUpdate;
import com.ticketmaster.ticketmaster.model.Booking;
import com.ticketmaster.ticketmaster.model.BookingStatus;
import com.ticketmaster.ticketmaster.model.Ticket;
import com.ticketmaster.ticketmaster.model.TicketStatus;
import com.ticketmaster.ticketmaster.repository.BookingRepository;
import com.ticketmaster.ticketmaster.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final StringRedisTemplate redisTemplate;

    private final SeatUpdateEmitter seatUpdateEmitter;

    private final WaitingQueueService waitingQueueService;

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    public Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    public Booking reserveTickets(UUID eventId, BookingRequest request) {

        if (!waitingQueueService.isAdmitted(eventId, request.getUserEmail())) {
            throw new RuntimeException("You must wait in the queue before booking");
        }

        List<Ticket> tickets = new ArrayList<>();
        List<String> acquiredLocks = new ArrayList<>();

        try {
            for (UUID ticketId : request.getTicketIds()) {
                String lockKey = "ticket:lock:" + ticketId;
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, request.getUserEmail(), LOCK_TTL);

                if (acquired == null || !acquired) {
                    throw new RuntimeException("Ticket " + ticketId + " is already reserved");
                }

                acquiredLocks.add(lockKey);

                Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

                if (ticket.getStatus() != TicketStatus.AVAILABLE) {
                    throw new RuntimeException("Ticket " + ticketId + " is not available");
                }

                ticket.setStatus(TicketStatus.RESERVED);
                ticketRepository.save(ticket);

                tickets.add(ticket);
            }

            BigDecimal total = tickets.stream()
                    .map(Ticket::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Booking booking = Booking.builder()
                    .userEmail(request.getUserEmail())
                    .eventId(eventId)
                    .totalPrice(total)
                    .status(BookingStatus.IN_PROGRESS)
                    .tickets(tickets)
                    .createdAt(LocalDateTime.now())
                    .build();

            // broadcast reserved status to all viewers
            for (Ticket ticket : tickets) {
                seatUpdateEmitter.broadcast(eventId,
                        SeatUpdate.builder().ticketId(ticket.getId()).status("RESERVED").build());
            }

            return bookingRepository.save(booking);
        }
        catch (Exception e) {
            for (String lockKey : acquiredLocks) {
                redisTemplate.delete(lockKey);
            }
            for (Ticket ticket : tickets) {
                ticket.setStatus(TicketStatus.AVAILABLE);
                ticketRepository.save(ticket);
            }
            // broadcast that seats are available again
            for (UUID ticketId : request.getTicketIds()) {
                seatUpdateEmitter.broadcast(eventId,
                        SeatUpdate.builder().ticketId(ticketId).status("AVAILABLE").build());
            }
            throw e;
        }
    }

    @Transactional
    public Booking confirmBooking(UUID bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if(booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new RuntimeException("Booking is not in progress");
        }

        for (Ticket ticket : booking.getTickets()) {
            String lockKey = "ticket:lock:" + ticket.getId();
            String lockOwner = redisTemplate.opsForValue().get(lockKey);

            if (!userEmail.equals(lockOwner)) {
                throw new RuntimeException("Reservation expired or belongs to another user");
            }

            ticket.setStatus(TicketStatus.BOOKED);
            seatUpdateEmitter.broadcast(booking.getEventId(), SeatUpdate.builder().ticketId(ticket.getId()).status("BOOKED").build());
            ticketRepository.save(ticket);

            redisTemplate.delete(lockKey);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }
}
