package com.ticketmaster.ticketmaster.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY = "queue:event:";
    private static final String ADMITTED_KEY = "admitted:event:";
    private static final int BATCH_SIZE = 10;
    private static final Duration ADMITTED_TTL = Duration.ofMinutes(15);

    private final Map<String, List<SseEmitter>> queueEmitters = new ConcurrentHashMap<>();

    public long joinQueue(UUID eventId, String userEmail) {
        String key = QUEUE_KEY + eventId;
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(key, userEmail, score);
        Long rank = redisTemplate.opsForZSet().rank(key, userEmail);
        return rank != null ? rank + 1 : 1;
    }

    public boolean isAdmitted(UUID eventId, String userEmail) {
        String key = ADMITTED_KEY + eventId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userEmail));
    }

    public long getPosition(UUID eventId, String userEmail) {
        String key = QUEUE_KEY + eventId;
        Long rank = redisTemplate.opsForZSet().rank(key, userEmail);
        return rank != null ? rank + 1 : 0;
    }

    public SseEmitter subscribeToQueue(UUID eventId, String userEmail) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String emitterKey = eventId + ":" + userEmail;

        queueEmitters.computeIfAbsent(emitterKey, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(emitterKey, emitter));
        emitter.onTimeout(() -> removeEmitter(emitterKey, emitter));
        emitter.onError(e -> removeEmitter(emitterKey, emitter));

        // send initial position
        try {
            long position = getPosition(eventId, userEmail);
            emitter.send(SseEmitter.event().name("queue-update").data(Map.of(
                    "position", position,
                    "admitted", false
            )));
        } catch (IOException e) {
            emitter.complete();
        }

        return emitter;
    }

    @Scheduled(fixedRate = 5000)
    public void admitNextBatch() {
        Set<String> queueKeys = redisTemplate.keys(QUEUE_KEY + "*");
        if (queueKeys == null) return;

        for (String queueKey : queueKeys) {
            String eventId = queueKey.replace(QUEUE_KEY, "");
            Set<String> batch = redisTemplate.opsForZSet().range(queueKey, 0, BATCH_SIZE - 1);
            if (batch == null || batch.isEmpty()) continue;

            String admittedKey = ADMITTED_KEY + eventId;
            for (String userEmail : batch) {
                redisTemplate.opsForSet().add(admittedKey, userEmail);
                redisTemplate.opsForZSet().remove(queueKey, userEmail);

                // notify user they're admitted
                String emitterKey = eventId + ":" + userEmail;
                notifyUser(emitterKey, Map.of("position", 0, "admitted", true));
            }
            redisTemplate.expire(admittedKey, ADMITTED_TTL);

            // notify remaining users of updated positions
            Set<String> remaining = redisTemplate.opsForZSet().range(queueKey, 0, -1);
            if (remaining != null) {
                long pos = 1;
                for (String userEmail : remaining) {
                    String emitterKey = eventId + ":" + userEmail;
                    notifyUser(emitterKey, Map.of("position", pos, "admitted", false));
                    pos++;
                }
            }
        }
    }

    private void notifyUser(String emitterKey, Map<String, Object> data) {
        List<SseEmitter> emitters = queueEmitters.get(emitterKey);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("queue-update").data(data));
            } catch (IOException e) {
                emitter.complete();
            }
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> emitters = queueEmitters.get(key);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) queueEmitters.remove(key);
        }
    }
}