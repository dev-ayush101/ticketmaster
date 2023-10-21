package com.ticketmaster.ticketmaster;

import com.ticketmaster.ticketmaster.dto.BookingRequest;
import com.ticketmaster.ticketmaster.model.Ticket;
import com.ticketmaster.ticketmaster.repository.TicketRepository;
import com.ticketmaster.ticketmaster.service.BookingService;
import com.ticketmaster.ticketmaster.service.WaitingQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private WaitingQueueService waitingQueueService;

    private static final UUID EVENT_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");

    @Test
    void onlyOneUserCanBookSameSeat() throws InterruptedException {
        // pick an available ticket
        Ticket ticket = ticketRepository.findByEventId(EVENT_ID).stream()
                .filter(t -> t.getStatus().name().equals("AVAILABLE"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No available tickets"));

        UUID ticketId = ticket.getId();
        int numUsers = 10;

        // admit all users first
        for (int i = 0; i < numUsers; i++) {
            String email = "user" + i + "@test.com";
            waitingQueueService.joinQueue(EVENT_ID, email);
        }
        // wait for batch job to admit them
        Thread.sleep(6000);

        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < numUsers; i++) {
            final String email = "user" + i + "@test.com";
            executor.submit(() -> {
                try {
                    latch.await(); // all threads start at the same time
                    BookingRequest request = new BookingRequest();
                    request.setTicketIds(List.of(ticketId));
                    request.setUserEmail(email);
                    bookingService.reserveTickets(EVENT_ID, request);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }

        latch.countDown(); // fire all threads simultaneously
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Threads should complete within 10 seconds");

        // exactly 1 user should succeed, rest should fail
        assertEquals(1, successes.get(), "Exactly one user should reserve the ticket");
        assertEquals(numUsers - 1, failures.get(), "All other users should be rejected");

        System.out.println("SUCCESS: 1 booking, " + failures.get() + " rejections — no double booking");
    }
}