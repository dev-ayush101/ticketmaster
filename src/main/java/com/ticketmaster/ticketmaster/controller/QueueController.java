package com.ticketmaster.ticketmaster.controller;

import com.ticketmaster.ticketmaster.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final WaitingQueueService waitingQueueService;

    @PostMapping("/{eventId}/join")
    public Map<String, Object> joinQueue(@PathVariable UUID eventId, @RequestParam String userEmail) {
        long position = waitingQueueService.joinQueue(eventId, userEmail);
        return Map.of("position", position, "admitted", false);
    }

    @GetMapping("/{eventId}/status")
    public Map<String, Object> getStatus(@PathVariable UUID eventId,
                                         @RequestParam String userEmail) {
        boolean admitted = waitingQueueService.isAdmitted(eventId, userEmail);
        long position = waitingQueueService.getPosition(eventId, userEmail);
        return Map.of("position", position, "admitted", admitted);
    }

    @GetMapping(value = "/{eventId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQueue(@PathVariable UUID eventId, @RequestParam String userEmail) {
        return waitingQueueService.subscribeToQueue(eventId, userEmail);
    }
}