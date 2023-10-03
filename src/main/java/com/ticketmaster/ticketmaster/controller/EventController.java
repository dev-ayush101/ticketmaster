package com.ticketmaster.ticketmaster.controller;

import com.ticketmaster.ticketmaster.dto.EventDetailResponse;
import com.ticketmaster.ticketmaster.model.Event;
import com.ticketmaster.ticketmaster.service.EventService;
import com.ticketmaster.ticketmaster.service.SeatUpdateEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final SeatUpdateEmitter seatUpdateEmitter;

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping(value = "/events/{eventId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSeatUpdates(@PathVariable UUID eventId) {
        return seatUpdateEmitter.subscribe(eventId);
    }

    @GetMapping("/events/search")
    public ResponseEntity<Page<Event>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.search(keyword, start, end, page, size));
    }
}