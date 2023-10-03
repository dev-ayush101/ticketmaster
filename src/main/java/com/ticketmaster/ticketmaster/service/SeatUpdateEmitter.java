package com.ticketmaster.ticketmaster.service;

import com.ticketmaster.ticketmaster.model.Booking;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SeatUpdateEmitter {

    // eventId -> list of open SSE connections
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID eventId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(eventId, emitter));
        emitter.onTimeout(() -> removeEmitter(eventId, emitter));
        emitter.onError(e -> removeEmitter(eventId, emitter));

        return emitter;
    }

    public void broadcast(UUID eventId, Object update) {
        List<SseEmitter> eventEmitters = emitters.get(eventId);
        if (eventEmitters == null) return;

        List<SseEmitter> dead = new java.util.ArrayList<>();

        for (SseEmitter emitter : eventEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("seat-update")
                        .data(update));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }

        eventEmitters.removeAll(dead);
    }

    private void removeEmitter(UUID eventId, SseEmitter emitter) {
        List<SseEmitter> eventEmitters = emitters.get(eventId);
        if (eventEmitters != null) {
            eventEmitters.remove(emitter);
        }
    }
}
