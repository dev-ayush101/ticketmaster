package com.ticketmaster.ticketmaster.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ticketmaster.ticketmaster.model.Event;
import com.ticketmaster.ticketmaster.model.EventDocument;
import com.ticketmaster.ticketmaster.repository.EventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final ElasticsearchClient esClient;
    private final EventRepository eventRepository;

    private static final String INDEX = "events";

    @PostConstruct
    public void syncOnStartup() {
        try {
            boolean indexExists = esClient.indices().exists(e -> e.index(INDEX)).value();
            if (!indexExists) {
                esClient.indices().create(c -> c.index(INDEX));
                log.info("Created ES index: {}", INDEX);
            }
            syncAllEvents();
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index", e);
        }
    }

    public void syncAllEvents() {
        List<Event> events = eventRepository.findAll();
        if (events.isEmpty()) return;

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Event event : events) {
                EventDocument doc = toDocument(event);
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(INDEX)
                                .id(doc.getId())
                                .document(doc)));
            }
            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                log.error("Bulk sync had errors");
            } else {
                log.info("Synced {} events to Elasticsearch", events.size());
            }
        } catch (IOException e) {
            log.error("Failed to sync events to ES", e);
        }
    }

    public List<EventDocument> search(String keyword) {
        try {
            SearchResponse<EventDocument> response = esClient.search(s -> s
                            .index(INDEX)
                            .query(q -> q
                                    .multiMatch(m -> m
                                            .query(keyword)
                                            .fields("name", "description", "performerName", "venueName", "eventType")
                                            .fuzziness("AUTO"))),
                    EventDocument.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (IOException e) {
            log.error("ES search failed", e);
            return List.of();
        }
    }

    private EventDocument toDocument(Event event) {
        return EventDocument.builder()
                .id(event.getId().toString())
                .name(event.getName())
                .description(event.getDescription())
                .performerName(event.getPerformer().getName())
                .venueName(event.getVenue().getName())
                .eventType(event.getEventType())
                .eventDate(event.getEventDate().toString())
                .build();
    }
}