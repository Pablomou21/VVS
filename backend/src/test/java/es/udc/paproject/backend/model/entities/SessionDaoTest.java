package es.udc.paproject.backend.model.entities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class SessionDaoTest {

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private RoomDao roomDao;

    @Autowired
    private TestEntityManager entityManager;

    private Session createSession(Movie movie, Room room, LocalDateTime date) {
        return sessionDao.save(new Session(movie, room, date, BigDecimal.TEN));
    }

    // Partición válida: orden por título y, dentro del título, por fecha ascendente
    @Test
    public void findByDateBetweenOrdersByMovieTitleThenDate() {
        Movie alpha = movieDao.save(new Movie("Alpha", "Summary", (short) 120));
        Movie zulu = movieDao.save(new Movie("Zulu", "Summary", (short) 120));
        Room room = roomDao.save(new Room("Room", 100));
        LocalDateTime firstDate = LocalDateTime.of(2040, 1, 1, 10, 0);
        Session zuluSession = createSession(zulu, room, firstDate.plusHours(1));
        Session alphaLaterSession = createSession(alpha, room, firstDate.plusHours(2));
        Session alphaEarlierSession = createSession(alpha, room, firstDate);
        entityManager.flush();
        entityManager.clear();

        List<Long> foundIds = sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(
                        firstDate, firstDate.plusHours(3))
                .stream()
                .map(Session::getId)
                .toList();

        assertEquals(List.of(alphaEarlierSession.getId(), alphaLaterSession.getId(), zuluSession.getId()),
                foundIds);
    }

    // Valores frontera: el rango incluye sesiones en sus dos extremos y excluye las exteriores
    @Test
    public void findByDateBetweenIncludesBothBoundaries() {
        Movie movie = movieDao.save(new Movie("Movie", "Summary", (short) 120));
        Room room = roomDao.save(new Room("Room", 100));
        LocalDateTime start = LocalDateTime.of(2041, 2, 3, 10, 0);
        createSession(movie, room, start.minusSeconds(1));
        Session atStart = createSession(movie, room, start);
        Session atEnd = createSession(movie, room, start.plusHours(1));
        createSession(movie, room, start.plusHours(1).plusSeconds(1));
        entityManager.flush();
        entityManager.clear();

        List<Long> foundIds = sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(
                        start, start.plusHours(1))
                .stream()
                .map(Session::getId)
                .toList();

        assertEquals(List.of(atStart.getId(), atEnd.getId()), foundIds);
    }

    // Partición inválida: un intervalo sin sesiones devuelve una lista vacía
    @Test
    public void findByDateBetweenReturnsEmptyWhenNoSessionsMatch() {
        LocalDateTime start = LocalDateTime.of(2042, 3, 4, 10, 0);

        assertTrue(sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(start, start.plusHours(1)).isEmpty());
    }
}
