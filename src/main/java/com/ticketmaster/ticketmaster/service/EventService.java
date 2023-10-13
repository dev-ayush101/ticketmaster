package com.ticketmaster.ticketmaster.service;

import com.ticketmaster.ticketmaster.dto.EventDetailResponse;
import com.ticketmaster.ticketmaster.model.Event;
import com.ticketmaster.ticketmaster.model.EventDocument;
import com.ticketmaster.ticketmaster.repository.EventRepository;
import com.ticketmaster.ticketmaster.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final EventSearchService eventSearchService;

    public EventDetailResponse getEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return EventDetailResponse.builder()
                .event(event)
                .tickets(ticketRepository.findByEventId(eventId))
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