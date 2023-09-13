-- Venues
INSERT INTO venues (id, name, address, capacity, seat_map, created_at) VALUES
    ('a1111111-1111-1111-1111-111111111111', 'Madison Square Garden', '4 Pennsylvania Plaza, New York, NY', 80,
    '{"sections": [{"name": "A", "rows": ["1","2","3","4","5","6","7","8"], "seatsPerRow": 10}]}', '2023-09-13 10:00:00'),
    ('a2222222-2222-2222-2222-222222222222', 'Wembley Stadium', 'London, HA9 0WS, UK', 80,
    '{"sections": [{"name": "A", "rows": ["1","2","3","4","5","6","7","8"], "seatsPerRow": 10}]}', '2023-09-13 10:00:00');

-- Performers
INSERT INTO performers (id, name, description, image_url, created_at) VALUES
    ('b1111111-1111-1111-1111-111111111111', 'Taylor Swift', 'American singer-songwriter', 'https://example.com/taylor.jpg', '2023-09-13 10:00:00'),
    ('b2222222-2222-2222-2222-222222222222', 'Coldplay', 'British rock band', 'https://example.com/coldplay.jpg', '2023-09-13 10:00:00'),
    ('b3333333-3333-3333-3333-333333333333', 'Arijit Singh', 'Indian playback singer', 'https://example.com/arijit.jpg', '2023-09-13 10:00:00');

-- Events
INSERT INTO events (id, name, description, event_date, event_type, venue_id, performer_id, created_at) VALUES
    ('c1111111-1111-1111-1111-111111111111', 'Eras Tour NYC', 'Taylor Swift live in New York', '2023-12-15 19:00:00', 'CONCERT',
    'a1111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', '2023-09-13 12:00:00'),
    ('c2222222-2222-2222-2222-222222222222', 'Music of the Spheres', 'Coldplay world tour London show', '2024-01-20 20:00:00', 'CONCERT',
    'a2222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222222', '2023-09-13 12:00:00'),
    ('c3333333-3333-3333-3333-333333333333', 'Arijit Live NYC', 'Arijit Singh live in concert', '2024-02-07 18:30:00', 'CONCERT',
    'a1111111-1111-1111-1111-111111111111', 'b3333333-3333-3333-3333-333333333333', '2023-09-13 12:00:00');

-- Tickets for Eras Tour NYC (8 rows x 10 seats = 80 tickets)
INSERT INTO tickets (id, event_id, section, row_name, seat_number, price, status, created_at)
SELECT
    gen_random_uuid(),
    'c1111111-1111-1111-1111-111111111111',
    'A',
    r.row_name,
    s.seat,
    CASE WHEN r.row_num <= 3 THEN 250.00 WHEN r.row_num <= 6 THEN 150.00 ELSE 80.00 END,
    'AVAILABLE',
    '2023-09-13 12:00:00'
FROM
    (VALUES ('1',1),('2',2),('3',3),('4',4),('5',5),('6',6),('7',7),('8',8)) AS r(row_name, row_num),
    generate_series(1, 10) AS s(seat);

-- Tickets for Coldplay London
INSERT INTO tickets (id, event_id, section, row_name, seat_number, price, status, created_at)
SELECT
    gen_random_uuid(),
    'c2222222-2222-2222-2222-222222222222',
    'A',
    r.row_name,
    s.seat,
    CASE WHEN r.row_num <= 3 THEN 200.00 WHEN r.row_num <= 6 THEN 120.00 ELSE 60.00 END,
    'AVAILABLE',
    '2023-09-13 12:00:00'
FROM
    (VALUES ('1',1),('2',2),('3',3),('4',4),('5',5),('6',6),('7',7),('8',8)) AS r(row_name, row_num),
    generate_series(1, 10) AS s(seat);

-- Tickets for Arijit NYC
INSERT INTO tickets (id, event_id, section, row_name, seat_number, price, status, created_at)
SELECT
    gen_random_uuid(),
    'c3333333-3333-3333-3333-333333333333',
    'A',
    r.row_name,
    s.seat,
    CASE WHEN r.row_num <= 3 THEN 180.00 WHEN r.row_num <= 6 THEN 100.00 ELSE 50.00 END,
    'AVAILABLE',
    '2023-09-13 12:00:00'
FROM
    (VALUES ('1',1),('2',2),('3',3),('4',4),('5',5),('6',6),('7',7),('8',8)) AS r(row_name, row_num),
    generate_series(1, 10) AS s(seat);