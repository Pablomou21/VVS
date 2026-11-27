package es.udc.paproject.backend.model.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.MovieDao;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.RoomDao;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.exceptions.DayOutOfRangeException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MovieServiceIntegrationTest {

    private static final Long NON_EXISTENT_ID = -1L;

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private RoomDao roomDao;

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private EntityManager entityManager;

    private Movie movie1;
    private Movie movie2;
    private Room room;
    private Session sessionToday;
    private Session sessionFuture;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Session createSession(Movie movie, LocalDateTime date) {
        Session session = new Session(movie, room, date, new BigDecimal("7.50"));
        return sessionDao.save(session);
    }

    @BeforeEach
    public void setUp() {
        movie1 = new Movie("Avatar", "Summary Avatar", (short) 160);
        movieDao.save(movie1);

        movie2 = new Movie("Batman", "Summary Batman", (short) 140);
        movieDao.save(movie2);

        room = new Room("Room 1", 50);
        roomDao.save(room);

        sessionToday = new Session(movie1, room, LocalDate.now().atTime(23, 59, 59), new BigDecimal("7.50"));
        sessionDao.save(sessionToday);

        sessionFuture = new Session(movie2, room, LocalDate.now().plusDays(6).atTime(12, 0), new BigDecimal("8.00"));
        sessionDao.save(sessionFuture);

        flushAndClear();
    }

    // -------------------------------------------------------------------------
    // Pruebas para viewBillboard
    // -------------------------------------------------------------------------

    /* Valor frontera: Comprobación de que se pueden consultar las sesiones del día actual (dayOffset = 0) */
    @Test
    public void testViewBillboardDayZero() throws Exception {
        List<BillboardEntry> billboard = movieService.viewBillboard(0);

        assertEquals(1, billboard.size());
    }

    /* Valor frontera: Comprobación de que se pueden consultar las sesiones del día límite permitido (dayOffset = 6) */
    @Test
    public void testViewBillboardDaySix() throws Exception {
        List<BillboardEntry> billboard = movieService.viewBillboard(6);

        assertEquals(1, billboard.size());
    }

    /* Valor frontera / Entrada inválida: Comprobación de que dayOffset negativo (-1) lanza DayOutOfRangeException */
    @Test
    public void testViewBillboardNegativeDay() {
        assertThrows(DayOutOfRangeException.class, () ->
            movieService.viewBillboard(-1)
        );
    }

    /* Valor frontera / Entrada inválida: Comprobación de que dayOffset mayor que 6 (7) lanza DayOutOfRangeException */
    @Test
    public void testViewBillboardDayGreaterThanSix() {
        assertThrows(DayOutOfRangeException.class, () ->
            movieService.viewBillboard(7)
        );
    }

    /* Partición equivalente: Comprobación de que devuelve una lista vacía cuando no hay sesiones para un día válido */
    @Test
    public void testViewBillboardEmptyDay() throws Exception {
        List<BillboardEntry> billboard = movieService.viewBillboard(3);

        assertTrue(billboard.isEmpty());
    }

    /* Partición equivalente: Comprobación de que, con varias películas el mismo día, la primera entrada es la de título alfabéticamente menor */
    @Test
    public void testViewBillboardOrdersMoviesByTitle() throws Exception {
        LocalDate day = LocalDate.now().plusDays(2);
        createSession(movie2, day.atTime(10, 0));
        createSession(movie1, day.atTime(20, 0));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(2);

        assertEquals("Avatar", billboard.get(0).getMovie().getTitle());
    }

    /* Partición equivalente: Comprobación de que varias sesiones de una misma película el mismo día se agrupan en una sola entrada */
    @Test
    public void testViewBillboardGroupsSessionsOfSameMovie() throws Exception {
        LocalDate day = LocalDate.now().plusDays(2);
        createSession(movie1, day.atTime(10, 0));
        createSession(movie1, day.atTime(16, 0));
        createSession(movie1, day.atTime(22, 0));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(2);

        assertEquals(3, billboard.get(0).getSessions().size());
    }

    /* Partición equivalente: Comprobación de que, con dos películas el mismo día, se devuelve una entrada por película */
    @Test
    public void testViewBillboardOneEntryPerMovie() throws Exception {
        LocalDate day = LocalDate.now().plusDays(2);
        createSession(movie1, day.atTime(10, 0));
        createSession(movie2, day.atTime(12, 0));
        createSession(movie1, day.atTime(18, 0));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(2);

        assertEquals(2, billboard.size());
    }

    /* Partición equivalente: Comprobación de que las sesiones de una misma película se ordenan por fecha ascendente */
    @Test
    public void testViewBillboardOrdersSessionsByDate() throws Exception {
        LocalDate day = LocalDate.now().plusDays(2);
        createSession(movie1, day.atTime(22, 0));
        createSession(movie1, day.atTime(10, 0));
        createSession(movie1, day.atTime(16, 0));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(2);

        assertEquals(day.atTime(10, 0), billboard.get(0).getSessions().get(0).getDate());
    }

    /* Valor frontera: Comprobación de que una sesión de hoy que empezó hace 1 minuto no aparece en la cartelera del día 0 */
    @Test
    public void testViewBillboardDayZeroExcludesPastSessionOfToday() throws Exception {
        Session pastSession = createSession(movie2, LocalDateTime.now().minusMinutes(1));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(0);

        assertTrue(billboard.stream().flatMap(entry -> entry.getSessions().stream())
            .noneMatch(session -> session.getId().equals(pastSession.getId())));
    }

    /* Valor frontera: Comprobación de que una sesión a las 00:00:00 del día N aparece en la cartelera del día N (inicio del rango) */
    @Test
    public void testViewBillboardIncludesSessionAtStartOfDay() throws Exception {
        createSession(movie1, LocalDate.now().plusDays(4).atStartOfDay());
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(4);

        assertEquals(1, billboard.size());
    }

    /* Valor frontera: Comprobación de que una sesión a las 23:59:59 del día N aparece en la cartelera del día N (fin del rango) */
    @Test
    public void testViewBillboardIncludesSessionAtEndOfDay() throws Exception {
        createSession(movie1, LocalDate.now().plusDays(4).atTime(23, 59, 59));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(4);

        assertEquals(1, billboard.size());
    }

    /* Valor frontera / Bug detectado (Issue #16): Comprobación de que una sesión a las 00:00:00 del día N+1 no aparece en la cartelera del día N */
    @Test
    public void testViewBillboardExcludesSessionAtStartOfNextDay() throws Exception {
        createSession(movie1, LocalDate.now().plusDays(5).atStartOfDay());
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(4);

        assertTrue(billboard.isEmpty());
    }

    /* Valor frontera: Comprobación de que una sesión a las 23:59:59 del día N-1 no aparece en la cartelera del día N */
    @Test
    public void testViewBillboardExcludesSessionAtEndOfPreviousDay() throws Exception {
        createSession(movie1, LocalDate.now().plusDays(3).atTime(23, 59, 59));
        flushAndClear();

        List<BillboardEntry> billboard = movieService.viewBillboard(4);

        assertTrue(billboard.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Pruebas para findMovieById
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que se recupera correctamente una película existente por su ID */
    @Test
    public void testFindMovieByIdSuccess() throws Exception {
        Movie foundMovie = movieService.findMovieById(movie1.getId());

        assertEquals(movie1.getId(), foundMovie.getId());
    }

    /* Caso de error / ID Inexistente: Comprobación de que se lanza InstanceNotFoundException al buscar una película que no existe */
    @Test
    public void testFindMovieByIdNonExistent() {
        assertThrows(InstanceNotFoundException.class, () ->
            movieService.findMovieById(NON_EXISTENT_ID)
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas para findSessionById
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que se recupera correctamente una sesión futura por su ID */
    @Test
    public void testFindSessionByIdSuccess() throws Exception {
        Session foundSession = movieService.findSessionById(sessionFuture.getId());

        assertEquals(sessionFuture.getId(), foundSession.getId());
    }

    /* Caso de error / ID Inexistente: Comprobación de que se lanza InstanceNotFoundException al buscar una sesión que no existe */
    @Test
    public void testFindSessionByIdNonExistent() {
        assertThrows(InstanceNotFoundException.class, () ->
            movieService.findSessionById(NON_EXISTENT_ID)
        );
    }

    /* Valor frontera / Lógica de negocio: Comprobación de que buscar una sesión que comenzó hace solo 1 segundo lanza SessionAlreadyStartedException */
    @Test
    public void testFindSessionByIdAlreadyStarted() {
        Session pastSession = new Session(movie1, sessionToday.getRoom(),
                LocalDateTime.now().minusSeconds(1), new BigDecimal("7.50"));
        sessionDao.save(pastSession);

        assertThrows(SessionAlreadyStartedException.class, () ->
            movieService.findSessionById(pastSession.getId())
        );
    }
}