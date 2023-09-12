CREATE TABLE venues (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(500),
                        capacity INT NOT NULL,
                        seat_map JSONB NOT NULL,
                        created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE performers (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(255) NOT NULL,
                            description TEXT,
                            image_url VARCHAR(500),
                            created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE events (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        event_date TIMESTAMP NOT NULL,
                        event_type VARCHAR(50),
                        venue_id UUID NOT NULL REFERENCES venues(id),
                        performer_id UUID NOT NULL REFERENCES performers(id),
                        created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tickets (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         event_id UUID NOT NULL REFERENCES events(id),
                         section VARCHAR(10),
                         row_name VARCHAR(10),
                         seat_number INT NOT NULL,
                         price DECIMAL(10,2) NOT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                         reserved_until TIMESTAMP,
                         created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE bookings (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_email VARCHAR(255) NOT NULL,
                          event_id UUID NOT NULL REFERENCES events(id),
                          total_price DECIMAL(10,2) NOT NULL,
                          status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
                          created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE booking_tickets (
                                 booking_id UUID NOT NULL REFERENCES bookings(id),
                                 ticket_id UUID NOT NULL REFERENCES tickets(id),
                                 PRIMARY KEY (booking_id, ticket_id)
);

CREATE INDEX idx_tickets_event_status ON tickets(event_id, status);
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_events_venue ON events(venue_id);
CREATE INDEX idx_events_performer ON events(performer_id);