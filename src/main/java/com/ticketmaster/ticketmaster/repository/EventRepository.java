package com.ticketmaster.ticketmaster.repository;

import com.ticketmaster.ticketmaster.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query(value = "SELECT e.* FROM events e JOIN performers p ON p.id = e.performer_id " +
            "WHERE (e.name ILIKE '%' || :keyword || '%' " +
            "OR e.description ILIKE '%' || :keyword || '%' " +
            "OR p.name ILIKE '%' || :keyword || '%') " +
            "AND e.event_date BETWEEN :start AND :end",
            nativeQuery = true)
    Page<Event> searchWithKeywordAndDates(@Param("keyword") String keyword, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                          Pageable pageable);

    @Query(value = "SELECT e.* FROM events e JOIN performers p ON p.id = e.performer_id " +
            "WHERE e.name ILIKE '%' || :keyword || '%' " +
            "OR e.description ILIKE '%' || :keyword || '%' " +
            "OR p.name ILIKE '%' || :keyword || '%'",
            nativeQuery = true)
    Page<Event> searchWithKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM events WHERE event_date BETWEEN :start AND :end", nativeQuery = true)
    Page<Event> searchByDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
}