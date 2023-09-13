package com.ticketmaster.ticketmaster.service;

import com.ticketmaster.ticketmaster.dto.EventDetailResponse;
import com.ticketmaster.ticketmaster.model.Event;
import com.ticketmaster.ticketmaster.repository.EventRepository;
import com.ticketmaster.ticketmaster.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

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

        if (hasKeyword && hasDates) {
            return eventRepository.searchWithKeywordAndDates(keyword, start, end, pageable);
        } else if (hasKeyword) {
            return eventRepository.searchWithKeyword(keyword, pageable);
        } else if (hasDates) {
            return eventRepository.searchByDates(start, end, pageable);
        } else {
            return eventRepository.findAll(pageable);
        }
    }
}