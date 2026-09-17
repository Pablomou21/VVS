-- ----------------------------------------------------------------------------
-- Put here INSERT statements for inserting data required by the application
-- in the "paproject" database.
-------------------------------------------------------------------------------
-- Users
INSERT INTO User(userName, password, firstName, lastName, email, role)
    VALUES ('viewer', '$2a$10$v.js2jCaX3xoKvkR6E2pbugMmZDBPlCAz2gA7EOIZhbkvsPFew/5u',
            'viewer', 'viewer', 'viewer@email.com', 0);

INSERT INTO User(userName, password, firstName, lastName, email, role)
VALUES ('ticketseller', '$2a$10$v.js2jCaX3xoKvkR6E2pbugMmZDBPlCAz2gA7EOIZhbkvsPFew/5u',
        'ticketseller', 'ticketseller', 'ticketseller@email.com', 1);

INSERT INTO User(userName, password, firstName, lastName, email, role)
VALUES ('testviewer', '$2a$10$v.js2jCaX3xoKvkR6E2pbugMmZDBPlCAz2gA7EOIZhbkvsPFew/5u',
        'testviewer', 'testviewer', 'testviewer@email.com', 0);

INSERT INTO User(userName, password, firstName, lastName, email, role)
VALUES ('testticketseller', '$2a$10$v.js2jCaX3xoKvkR6E2pbugMmZDBPlCAz2gA7EOIZhbkvsPFew/5u',
        'testticketseller', 'testticketseller', 'testticketseller@email.com', 1);

-- Rooms
-- Room 1: 9 seats
-- Room 2: 50 seats
INSERT INTO Room(name, capacity) VALUES ('Room 1', 9);

INSERT INTO Room(name, capacity) VALUES ('Room 2', 50);

-- Movies
INSERT INTO Movie(title, summary, duration)
    VALUES ('Movie 1', 'Summary of Movie 1.', 120);

INSERT INTO Movie(title, summary, duration)
VALUES ('Movie 2', 'Summary of Movie 2.', 90);

-- Sessions
-- Today (Day 0)
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
    VALUES (2, 2, DATE_ADD(DATE(NOW()), INTERVAL '0 00:05' DAY_MINUTE), 8.99, 43, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
    VALUES (1, 1, DATE_ADD(DATE(NOW()), INTERVAL '0 23:55' DAY_MINUTE), 10.50, 9, 0);

-- Day +1
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 1, DATE_ADD(DATE(NOW()), INTERVAL '1 17:30' DAY_MINUTE), 8.99, 9, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (2, 2, DATE_ADD(DATE(NOW()), INTERVAL '1 19:30' DAY_MINUTE), 10.50, 50, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 2, DATE_ADD(DATE(NOW()), INTERVAL '1 01:00' DAY_MINUTE), 7.99, 48, 0);

-- Day +2
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 2, DATE_ADD(DATE(NOW()), INTERVAL '2 18:00' DAY_MINUTE), 9.75, 50, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (2, 1, DATE_ADD(DATE(NOW()), INTERVAL '2 21:00' DAY_MINUTE), 11.00, 9, 0);

-- Day +3
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (2, 2, DATE_ADD(DATE(NOW()), INTERVAL '3 20:00' DAY_MINUTE), 9.75, 50, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 1, DATE_ADD(DATE(NOW()), INTERVAL '3 22:00' DAY_MINUTE), 11.00, 9, 0);

-- Day +4
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 1, DATE_ADD(DATE(NOW()), INTERVAL '4 17:30' DAY_MINUTE), 9.75, 9, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (2, 2, DATE_ADD(DATE(NOW()), INTERVAL '4 19:30' DAY_MINUTE), 11.00, 50, 0);

-- Day +5
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 2, DATE_ADD(DATE(NOW()), INTERVAL '5 18:00' DAY_MINUTE), 8.99, 50, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (2, 1, DATE_ADD(DATE(NOW()), INTERVAL '5 21:00' DAY_MINUTE), 10.50, 9, 0);

-- Day +6
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (2, 2, DATE_ADD(DATE(NOW()), INTERVAL '6 20:00' DAY_MINUTE), 8.99, 50, 0);
INSERT INTO Session(movieId, roomId, date, price, freeSeats, version)
VALUES (1, 1, DATE_ADD(DATE(NOW()), INTERVAL '6 22:00' DAY_MINUTE), 10.50, 9, 0);

-- Orders
INSERT INTO OrderTable(units, sessionId, creditCardNum, date, delivered, userId)
    VALUES (5, 1, '1234567890123456', NOW(), false, 1);

INSERT INTO OrderTable(units, sessionId, creditCardNum, date, delivered, userId)
VALUES (2, 1, '6543210987654321', NOW(), false, 1);

INSERT INTO OrderTable(units, sessionId, creditCardNum, date, delivered, userId)
VALUES (2, 5, '1111111111111111', NOW(), false, 3);
