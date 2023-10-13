package com.ticketmaster.ticketmaster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDocument {
    private String id;
    private String name;
    private String description;
    private String performerName;
    private String venueName;
    private String eventType;
    private String eventDate;
}