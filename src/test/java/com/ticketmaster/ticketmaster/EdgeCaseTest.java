package com.ticketmaster.ticketmaster;

import com.ticketmaster.ticketmaster.dto.BookingRequest;
import com.ticketmaster.ticketmaster.model.Booking;
import com.ticketmaster.ticketmaster.model.Ticket;
import com.ticketmaster.ticketmaster.model.TicketStatus;
import com.ticketmaster.ticketmaster.repository.TicketRepository;
import com.ticketmaster.ticketmaster.service.BookingService;
import com.ticketmaster.ticketmaster.service.WaitingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EdgeCaseTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final UUID EVENT_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");

    @BeforeEach
    void cleanRedis() {
        // clear all ticket locks and reservation sets
        Set<String> keys = redisTemplate.keys("ticket:lock:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        redisTemplate.delete("event:" + EVENT_ID + ":reserved");
    }

    private Ticket getAvailableTicket() {
        return ticketRepository.findByEventId(EVENT_ID).stream()
                .filter(t -> t.getStatus() == TicketStatus.AVAILABLE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No available tickets"));
    }

    private void admitUser(String email) throws InterruptedException {
        waitingQueueService.joinQueue(EVENT_ID, email);
        Thread.sleep(6000);
    }

    @Test
    void rejectBookingForAlreadyBookedTicket() throws InterruptedException {
        Ticket ticket = getAvailableTicket();
        ticket.setStatus(TicketStatus.BOOKED);
        ticketRepository.save(ticket);

        String email = "booked@test.com";
        admitUser(email);

        BookingRequest request = new BookingRequest();
        request.setTicketIds(List.of(ticket.getId()));
        request.setUserEmail(email);

        Exception ex = assertThrows(RuntimeException.class,
                () -> bookingService.reserveTickets(EVENT_ID, request));
        assertTrue(ex.getMessage().contains("is not available"));

        // clean up
        ticket.setStatus(TicketStatus.AVAILABLE);
        ticketRepository.save(ticket);

        System.out.println("PASS: already booked ticket rejected");
    }

    @Test
    void rejectInvalidTicketId() throws InterruptedException {
        String email = "invalid@test.com";
        admitUser(email);

        UUID fakeTicketId = UUID.randomUUID();

        BookingRequest request = new BookingRequest();
        request.setTicketIds(List.of(fakeTicketId));
        request.setUserEmail(email);

        Exception ex = assertThrows(RuntimeException.class,
                () -> bookingService.reserveTickets(EVENT_ID, request));
        assertTrue(ex.getMessage().contains("Ticket not found"));

        System.out.println("PASS: invalid ticket ID rejected");
    }

    @Test
    void rejectDoubleConfirm() throws InterruptedException {
        String email = "doubleconfirm@test.com";
        admitUser(email);

        Ticket ticket = getAvailableTicket();

        BookingRequest request = new BookingRequest();
        request.setTicketIds(List.of(ticket.getId()));
        request.setUserEmail(email);

        Booking booking = bookingService.reserveTickets(EVENT_ID, request);
        bookingService.confirmBooking(booking.getId(), email);

        Exception ex = assertThrows(RuntimeException.class,
                () -> bookingService.confirmBooking(booking.getId(), email));
        assertTrue(ex.getMessage().contains("Booking is not in progress"));

        System.out.println("PASS: double confirm rejected");
    }
}