package es.udc.paproject.backend.test.model.services;

import es.udc.paproject.backend.model.entities.*;
import es.udc.paproject.backend.model.exceptions.DayOutOfRangeException;
import es.udc.paproject.backend.model.services.BillboardEntry;
import es.udc.paproject.backend.model.services.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MovieServiceTest {

    private final int NEGATIVE_DAY = -1;
    private final int OUT_OF_RANGE_DAY = 7;
    private final Long NON_EXISTENT_ID = Long.valueOf(-1);

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private RoomDao roomDao;



    private Session createSession(LocalDateTime date) {
        Movie movie = movieDao.save(new Movie("Movie 1", "Description", (short) 120));
        Room room = roomDao.save(new Room("Room 1", 100));
        Session session = new Session(movie, room, date, new BigDecimal("7.50"));
        return sessionDao.save(session);
    }

    private Movie createMovie(String title) {
            return new Movie (title, "summary",  (short) 90);
        }

    private Room createRoom() {
        return new Room("name", 200);
    }

    private Session createSession(Movie movie, Room room, LocalDateTime date) {
        return new Session(movie, room, date, new BigDecimal(1));
    }

    @Test
    public void testFindMovieById() throws InstanceNotFoundException {

        Movie movie = movieDao.save(new Movie("Movie 1", "Description", (short) 120));

        assertEquals(movie, movieService.findMovieById(movie.getId()));

    }

    @Test
    public void testFindMovieByNonExistentId() {
        assertThrows(InstanceNotFoundException.class, () -> movieService.findMovieById(NON_EXISTENT_ID));
    }

    @Test
    public void testFindSessionById() throws InstanceNotFoundException, SessionAlreadyStartedException {

        Session session = createSession(LocalDateTime.now().plusDays(1));

        assertEquals(session, movieService.findSessionById(session.getId()));

    }

    @Test
    public void testFindSessionByNonExistentId() {
        assertThrows(InstanceNotFoundException.class, () -> movieService.findSessionById(NON_EXISTENT_ID));
    }

    @Test
    public void testFindSessionByIdAlreadyStarted() {

        Session session = createSession(LocalDateTime.now().minusDays(1));

        assertThrows(SessionAlreadyStartedException.class, () -> movieService.findSessionById(session.getId()));

    }

    @Test
    public void testViewEmptyBillboard() throws DayOutOfRangeException {
        assertTrue(movieService.viewBillboard(0).isEmpty());
    }

    @Test
    public void testViewBillboardForToday() throws DayOutOfRangeException {
        Movie movie1 = createMovie("A");
        Movie movie2 = createMovie("B");

        movieDao.save(movie1);
        movieDao.save(movie2);

        Room room = createRoom();

        roomDao.save(room);

        LocalDate today = LocalDate.now();
        Session session1Movie1 = createSession(movie1, room, today.atTime(23,55));
        Session session2Movie1 = createSession(movie1, room, today.atTime(23,56));
        Session session3Movie1 = createSession(movie1, room, today.atTime(23,57));
        Session session1Movie2 = createSession(movie2, room, today.atTime(23,56));
        Session session2Movie2 = createSession(movie2, room, today.atTime(23,55));

        sessionDao.save(session1Movie1);
        sessionDao.save(session2Movie1);
        sessionDao.save(session3Movie1);
        sessionDao.save(session1Movie2);
        sessionDao.save(session2Movie2);

        List<BillboardEntry> billboardEntries = movieService.viewBillboard(0);

        assertEquals(2, billboardEntries.size());

        assertEquals(movie1, billboardEntries.get(0).getMovie());
        assertEquals(movie2, billboardEntries.get(1).getMovie());

        assertEquals(Arrays.asList(session1Movie1,session2Movie1,session3Movie1), billboardEntries.get(0).getSessions());
        assertEquals(Arrays.asList(session2Movie2,session1Movie2), billboardEntries.get(1).getSessions());

    }

    @Test
    public void testViewBillboardForAnotherDay() throws DayOutOfRangeException {
        Movie movie1 = createMovie("A");
        Movie movie2 = createMovie("B");

        movieDao.save(movie1);
        movieDao.save(movie2);

        Room room = createRoom();

        roomDao.save(room);

        LocalDate startTime1 = LocalDate.now().plusDays(3);
        LocalDate startTime2 = LocalDate.now().plusDays(6);
        Session session1Movie1 = createSession(movie1, room, startTime1.atTime(23,55));
        Session session2Movie1 = createSession(movie1, room, startTime1.atTime(23,56));
        Session session3Movie1 = createSession(movie1, room, startTime1.atTime(23,57));
        Session session1Movie2 = createSession(movie2, room, startTime2.atTime(23,56));
        Session session2Movie2 = createSession(movie2, room, startTime2.atTime(23,55));

        sessionDao.save(session1Movie1);
        sessionDao.save(session2Movie1);
        sessionDao.save(session3Movie1);
        sessionDao.save(session1Movie2);
        sessionDao.save(session2Movie2);

        List<BillboardEntry> billboardEntries3 = movieService.viewBillboard(3);
        List<BillboardEntry> billboardEntries6 = movieService.viewBillboard(6);

        assertEquals(1, billboardEntries3.size());
        assertEquals(1, billboardEntries6.size());

        assertEquals(movie1, billboardEntries3.getFirst().getMovie());
        assertEquals(movie2, billboardEntries6.getFirst().getMovie());

        assertEquals(Arrays.asList(session1Movie1,session2Movie1,session3Movie1), billboardEntries3.getFirst().getSessions());
        assertEquals(Arrays.asList(session2Movie2,session1Movie2), billboardEntries6.getFirst().getSessions());

    }

    @Test
    public void testViewBillboardForInvalidDay() {
        assertThrows(DayOutOfRangeException.class, () -> movieService.viewBillboard(NEGATIVE_DAY));
        assertThrows(DayOutOfRangeException.class, () -> movieService.viewBillboard(OUT_OF_RANGE_DAY));

    }


}
