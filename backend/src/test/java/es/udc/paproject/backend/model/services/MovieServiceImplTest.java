package es.udc.paproject.backend.model.services;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.MovieDao;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.exceptions.DayOutOfRangeException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import net.jqwik.api.Arbitraries;

@ExtendWith(MockitoExtension.class)
public class MovieServiceImplTest {

    @Mock
    private MovieDao movieDao;

    @Mock
    private SessionDao sessionDao;

    @InjectMocks
    private MovieServiceImpl movieService;

    private Long randomId() {
        return Arbitraries.longs().between(1, Long.MAX_VALUE).sample();
    }

    private int randomValidDay() {
        return Arbitraries.integers().between(0, 6).sample();
    }

    private Movie movie(Long id, String title) {
        Movie movie = new Movie(title, "Resumen", (short) 90);
        movie.setId(id);
        return movie;
    }

    private Session session(Movie movie, LocalDateTime date) {
        return new Session(movie, new Room(), date, BigDecimal.TEN);
    }

    private void daoReturns(List<Session> sessions) {
        when(sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(any(), any())).thenReturn(sessions);
    }

    // ========== viewBillboard: rango de días ==========

    // ---------- Partición inválida: día fuera de 0..6 ----------

    // Valor frontera: -1, justo por debajo del rango
    @Test
    public void viewBillboardWithDayMinusOneThrows() {
        assertThrows(DayOutOfRangeException.class, () -> movieService.viewBillboard(-1));
    }

    // Valor frontera: 7, justo por encima del rango
    @Test
    public void viewBillboardWithDaySevenThrows() {
        assertThrows(DayOutOfRangeException.class, () -> movieService.viewBillboard(7));
    }

    // Dato aleatorio: día entre Integer.MIN_VALUE y -1
    @Test
    public void viewBillboardWithNegativeDayThrows() {
        int day = Arbitraries.integers().between(Integer.MIN_VALUE, -1).sample();

        assertThrows(DayOutOfRangeException.class, () -> movieService.viewBillboard(day));
    }

    // Dato aleatorio: día entre 7 e Integer.MAX_VALUE
    @Test
    public void viewBillboardWithDayAfterWeekThrows() {
        int day = Arbitraries.integers().between(7, Integer.MAX_VALUE).sample();

        assertThrows(DayOutOfRangeException.class, () -> movieService.viewBillboard(day));
    }

    // Dato aleatorio: día fuera de rango, no se consulta la base de datos
    @Test
    public void viewBillboardWithDayOutOfRangeDoesNotQueryDao() {
        int day = Arbitraries.oneOf(Arbitraries.integers().between(Integer.MIN_VALUE, -1),
                Arbitraries.integers().between(7, Integer.MAX_VALUE)).sample();

        catchThrowable(() -> movieService.viewBillboard(day));

        verifyNoInteractions(sessionDao);
    }

    // ---------- Partición válida: día entre 0 y 6, sin sesiones ----------

    // Valor frontera: 0, el menor día válido
    @Test
    public void viewBillboardWithDayZeroAndNoSessionsReturnsEmptyList() throws DayOutOfRangeException {
        List<BillboardEntry> result = movieService.viewBillboard(0);

        assertTrue(result.isEmpty());
    }

    // Valor frontera: 6, el mayor día válido
    @Test
    public void viewBillboardWithDaySixAndNoSessionsReturnsEmptyList() throws DayOutOfRangeException {
        List<BillboardEntry> result = movieService.viewBillboard(6);

        assertTrue(result.isEmpty());
    }

    // ========== viewBillboard: intervalo que se pide al DAO ==========

    // Valor frontera: día 0, el intervalo empieza en el instante actual
    @Test
    public void viewBillboardTodayStartsNow() throws DayOutOfRangeException {
        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        when(sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(start.capture(), any())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();

        movieService.viewBillboard(0);

        LocalDateTime after = LocalDateTime.now();
        assertTrue(!start.getValue().isBefore(before) && !start.getValue().isAfter(after));
    }

    // Dato aleatorio: día entre 1 y 6, el intervalo empieza a las 00:00
    @Test
    public void viewBillboardOtherDayStartsAtMidnight() throws DayOutOfRangeException {
        int day = Arbitraries.integers().between(1, 6).sample();
        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        when(sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(start.capture(), any())).thenReturn(List.of());

        movieService.viewBillboard(day);

        assertEquals(LocalDate.now().plusDays(day).atStartOfDay(), start.getValue());
    }

    // Dato aleatorio: día entre 0 y 6, el intervalo acaba en el último instante del día
    @Test
    public void viewBillboardEndsAtEndOfDay() throws DayOutOfRangeException {
        int day = randomValidDay();
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        when(sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(any(), end.capture())).thenReturn(List.of());

        movieService.viewBillboard(day);

        assertEquals(LocalDate.now().plusDays(day).atTime(LocalTime.MAX), end.getValue());
    }

    // ========== viewBillboard: agrupación de sesiones por película ==========

    // Partición: una película con varias sesiones genera una sola entrada
    @Test
    public void viewBillboardGroupsSessionsOfSameMovie() throws DayOutOfRangeException {
        Movie movie = movie(1L, "A");
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        daoReturns(List.of(session(movie, date), session(movie, date.plusHours(2)),
                session(movie, date.plusHours(4))));

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(1, result.size());
    }

    // Valor frontera: una película con una sola sesión, la entrada contiene solo esa sesión
    @Test
    public void viewBillboardMovieWithOneSessionHasOnlyThatSession() throws DayOutOfRangeException {
        Session session = session(movie(1L, "A"), LocalDateTime.now().plusDays(1));
        daoReturns(List.of(session));

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(List.of(session), result.get(0).getSessions());
    }

    // Partición: la entrada de la película contiene todas sus sesiones en orden
    @Test
    public void viewBillboardEntryContainsAllSessionsOfMovie() throws DayOutOfRangeException {
        Movie movie = movie(1L, "A");
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        List<Session> sessions = List.of(session(movie, date), session(movie, date.plusHours(2)));
        daoReturns(sessions);

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(sessions, result.get(0).getSessions());
    }

    // Partición: dos películas generan dos entradas
    @Test
    public void viewBillboardCreatesOneEntryPerMovie() throws DayOutOfRangeException {
        Movie movieA = movie(1L, "A");
        Movie movieB = movie(2L, "B");
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        daoReturns(List.of(session(movieA, date), session(movieA, date.plusHours(2)), session(movieB, date)));

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(2, result.size());
    }

    // Partición: la segunda entrada es la segunda película
    @Test
    public void viewBillboardSecondEntryIsSecondMovie() throws DayOutOfRangeException {
        Movie movieA = movie(1L, "A");
        Movie movieB = movie(2L, "B");
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        daoReturns(List.of(session(movieA, date), session(movieA, date.plusHours(2)), session(movieB, date)));

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertSame(movieB, result.get(1).getMovie());
    }

    // Partición: la segunda entrada solo tiene las sesiones de la segunda película
    @Test
    public void viewBillboardSecondEntryOnlyHasItsSessions() throws DayOutOfRangeException {
        Movie movieA = movie(1L, "A");
        Movie movieB = movie(2L, "B");
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        Session sessionB = session(movieB, date);
        daoReturns(List.of(session(movieA, date), session(movieA, date.plusHours(2)), sessionB));

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(List.of(sessionB), result.get(1).getSessions());
    }

    // Partición: dos películas con el mismo título se agrupan por id, no por título
    @Test
    public void viewBillboardGroupsByMovieIdNotTitle() throws DayOutOfRangeException {
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        daoReturns(List.of(session(movie(1L, "A"), date), session(movie(2L, "A"), date)));

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(2, result.size());
    }

    // Datos aleatorios: de 1 a 5 películas con 1 a 4 sesiones cada una
    @Test
    public void viewBillboardEntriesMatchNumberOfMovies() throws DayOutOfRangeException {
        int movies = Arbitraries.integers().between(1, 5).sample();
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        List<Session> sessions = new ArrayList<>();
        for (long id = 1; id <= movies; id++) {
            Movie movie = movie(id, "Pelicula " + id);
            int movieSessions = Arbitraries.integers().between(1, 4).sample();
            for (int i = 0; i < movieSessions; i++) {
                sessions.add(session(movie, date.plusHours(i)));
            }
        }
        daoReturns(sessions);

        List<BillboardEntry> result = movieService.viewBillboard(1);

        assertEquals(movies, result.size());
    }

    // ========== findMovieById ==========

    // Partición válida. Dato aleatorio: id entre 1 y Long.MAX_VALUE
    @Test
    public void findMovieByIdReturnsMovie() throws InstanceNotFoundException {
        Long id = randomId();
        Movie movie = movie(id, "A");
        when(movieDao.findById(id)).thenReturn(Optional.of(movie));

        Movie result = movieService.findMovieById(id);

        assertSame(movie, result);
    }

    // Partición inválida. Dato aleatorio: id sin película
    @Test
    public void findMovieByIdWithNonExistentMovieThrows() {
        Long id = randomId();
        when(movieDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(InstanceNotFoundException.class, () -> movieService.findMovieById(id));
    }

    // Partición inválida. Dato aleatorio: id sin película
    @Test
    public void movieNotFoundExceptionKeyIsMovieId() {
        Long id = randomId();
        when(movieDao.findById(id)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> movieService.findMovieById(id),
                InstanceNotFoundException.class);

        assertEquals(id, e.getKey());
    }

    // Partición inválida. Dato aleatorio: id sin película
    @Test
    public void movieNotFoundExceptionNameIsMovieEntity() {
        Long id = randomId();
        when(movieDao.findById(id)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> movieService.findMovieById(id),
                InstanceNotFoundException.class);

        assertEquals("project.entities.movie", e.getName());
    }

    // ========== findSessionById ==========

    // ---------- Partición válida: sesión futura ----------

    // Dato aleatorio: sesión entre 1 hora y 30 días en el futuro
    @Test
    public void findSessionByIdReturnsFutureSession()
            throws InstanceNotFoundException, SessionAlreadyStartedException {
        Long id = randomId();
        long minutes = Arbitraries.longs().between(60, 30 * 24 * 60).sample();
        Session session = session(movie(1L, "A"), LocalDateTime.now().plusMinutes(minutes));
        when(sessionDao.findById(id)).thenReturn(Optional.of(session));

        Session result = movieService.findSessionById(id);

        assertSame(session, result);
    }

    // Valor frontera: sesión que empieza dentro de 1 minuto
    @Test
    public void findSessionByIdReturnsSessionAboutToStart()
            throws InstanceNotFoundException, SessionAlreadyStartedException {
        Long id = randomId();
        Session session = session(movie(1L, "A"), LocalDateTime.now().plusMinutes(1));
        when(sessionDao.findById(id)).thenReturn(Optional.of(session));

        Session result = movieService.findSessionById(id);

        assertSame(session, result);
    }

    // ---------- Partición inválida: sesión inexistente ----------

    // Dato aleatorio: id sin sesión
    @Test
    public void findSessionByIdWithNonExistentSessionThrows() {
        Long id = randomId();
        when(sessionDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(InstanceNotFoundException.class, () -> movieService.findSessionById(id));
    }

    // Dato aleatorio: id sin sesión
    @Test
    public void sessionNotFoundExceptionNameIsSessionEntity() {
        Long id = randomId();
        when(sessionDao.findById(id)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> movieService.findSessionById(id),
                InstanceNotFoundException.class);

        assertEquals("project.entities.session", e.getName());
    }

    // ---------- Partición inválida: sesión ya empezada ----------

    // Dato aleatorio: sesión entre 1 hora y 30 días en el pasado
    @Test
    public void findSessionByIdWithPastSessionThrows() {
        Long id = randomId();
        long minutes = Arbitraries.longs().between(60, 30 * 24 * 60).sample();
        Session session = session(movie(1L, "A"), LocalDateTime.now().minusMinutes(minutes));
        when(sessionDao.findById(id)).thenReturn(Optional.of(session));

        assertThrows(SessionAlreadyStartedException.class, () -> movieService.findSessionById(id));
    }

    // Valor frontera: sesión que empezó hace 1 minuto
    @Test
    public void findSessionByIdWithJustStartedSessionThrows() {
        Long id = randomId();
        Session session = session(movie(1L, "A"), LocalDateTime.now().minusMinutes(1));
        when(sessionDao.findById(id)).thenReturn(Optional.of(session));

        assertThrows(SessionAlreadyStartedException.class, () -> movieService.findSessionById(id));
    }

    // Dato aleatorio: sesión empezada, la excepción guarda su id
    @Test
    public void sessionAlreadyStartedExceptionKeepsSessionId() {
        Long id = randomId();
        Session session = session(movie(1L, "A"), LocalDateTime.now().minusHours(1));
        when(sessionDao.findById(id)).thenReturn(Optional.of(session));

        SessionAlreadyStartedException e = catchThrowableOfType(() -> movieService.findSessionById(id),
                SessionAlreadyStartedException.class);

        assertEquals(id, e.getSessionId());
    }

}
