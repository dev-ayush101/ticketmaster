package com.ticketmaster.ticketmaster.service;

import com.ticketmaster.ticketmaster.dto.EventDetailResponse;
import com.ticketmaster.ticketmaster.model.Event;
import com.ticketmaster.ticketmaster.model.EventDocument;
import com.ticketmaster.ticketmaster.model.Ticket;
import com.ticketmaster.ticketmaster.model.TicketStatus;
import com.ticketmaster.ticketmaster.repository.EventRepository;
import com.ticketmaster.ticketmaster.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final EventSearchService eventSearchService;

    private final StringRedisTemplate redisTemplate;

    public EventDetailResponse getEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        List<Ticket> tickets = ticketRepository.findByEventId(eventId);

        // clean expired entries, then overlay reserved status from Redis
        double now = System.currentTimeMillis();
        String reservedKey = "event:" + eventId + ":reserved";
        redisTemplate.opsForZSet().removeRangeByScore(reservedKey, 0, now);
        Set<String> reservedIds = redisTemplate.opsForZSet().rangeByScore(reservedKey, now, Double.MAX_VALUE);

        for (Ticket ticket : tickets) {
            if (reservedIds != null && reservedIds.contains(ticket.getId().toString())) {
                ticket.setStatus(TicketStatus.RESERVED);
            }
        }

        return EventDetailResponse.builder()
                .event(event)
                .tickets(tickets)
                .build();
    }

    public Page<Event> search(String keyword, LocalDateTime start,
                              LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        boolean hasKeyword = (keyword != null) && (!keyword.isBlank());
        boolean hasDates = (start != null) && (end != null);

        if (hasKeyword) {
            List<EventDocument> docs = eventSearchService.search(keyword);
            List<UUID> ids = docs.stream().map(d -> UUID.fromString(d.getId())).toList();
            if (ids.isEmpty()) return Page.empty(pageable);

            List<Event> events = eventRepository.findAllById(ids);

            if (hasDates) {
                events = events.stream()
                        .filter(e -> !e.getEventDate().isBefore(start) && !e.getEventDate().isAfter(end))
                        .toList();
            }
            return new PageImpl<>(events, pageable, events.size());
        }

        if (hasDates) {
            return eventRepository.searchByDates(start, end, pageable);
        }
        return eventRepository.findAll(pageable);
    }
}