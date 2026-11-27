package es.udc.paproject.backend.rest.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.testutil.PerformanceMeter;

public class MovieControllerIntegrationTest extends ControllerIntegrationTestBase {

    /* Requisito no funcional: una petición GET /movies/billboard/{day} debe responder de media en menos de 100 ms, incluyendo
     * la seguridad, el controlador, el servicio, la BD real y la serialización JSON.
     * Criterio: 100 ms es el límite para que el usuario perciba una respuesta como instantánea (J. Nielsen, "Response Times:
     * The 3 Important Limits"); a diferencia de las pruebas de servicio (50 ms), aquí se mide toda la cadena del backend */
    private static final double VIEW_BILLBOARD_REQUEST_THRESHOLD_MS = 100.0;

    /* Tamaño de la cartelera de la prueba de rendimiento: número de películas y de sesiones por película en el día medido */
    private static final int PERFORMANCE_MOVIES = 5;
    private static final int PERFORMANCE_SESSIONS_PER_MOVIE = 4;

    private Movie avatar;
    private Movie batman;
    private Room room;
    private Session sessionToday;

    @BeforeEach
    public void setUp() {
        avatar = createMovie("Avatar");
        batman = createMovie("Batman");
        room = createRoom(10);

        // Sesións fixadas temporalmente con LocalDate.now() para evitar tests intermitentes
        sessionToday = createSession(avatar, room, LocalDate.now().atTime(23, 59, 59));
        createSession(avatar, room, LocalDate.now().plusDays(6).atTime(10, 0));
        createSession(batman, room, LocalDate.now().plusDays(6).atTime(12, 0));
        createSession(avatar, room, LocalDate.now().plusDays(6).atTime(18, 0));
    }

    // -------------------------------------------------------------------------
    // Pruebas para GET /movies/{id}
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que consultar una película existente devuelve HTTP 200 OK sin autenticación */
    @Test
    public void testFindMovieByIdReturnsOk() throws Exception {
        mockMvc.perform(get("/movies/" + avatar.getId())).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la respuesta contiene los datos correctos de la película */
    @Test
    public void testFindMovieByIdReturnsTitle() throws Exception {
        mockMvc.perform(get("/movies/" + avatar.getId())).andExpect(jsonPath("$.title").value("Avatar"));
    }

    /* Partición equivalente: Comprobación de que consultar una película con token válido devuelve HTTP 200 OK */
    @Test
    public void testFindMovieByIdWithValidTokenReturnsOk() throws Exception {
        User user = createUser("viewer", User.RoleType.USER);

        mockMvc.perform(get("/movies/" + avatar.getId()).header(AUTHORIZATION, bearer(user))).andExpect(status().isOk());
    }

    /* Partición equivalente / Caso de error: Comprobación de que enviar un token inválido devuelve HTTP 401 Unauthorized */
    @Test
    public void testFindMovieByIdWithInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/movies/" + avatar.getId()).header(AUTHORIZATION, INVALID_TOKEN_HEADER))
            .andExpect(status().isUnauthorized());
    }

    /* Partición equivalente / Caso de error: Comprobación de que solicitar una película inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testFindMovieByIdNonExistentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/movies/" + NON_EXISTENT_ID)).andExpect(status().isNotFound());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un ID de película no numérico devuelve HTTP 400 Bad Request */
    @Test
    public void testFindMovieByIdNotANumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/movies/abc")).andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Caso de error: Comprobación de que realizar POST en /movies/1 devuelve HTTP 403 Forbidden */
    @Test
    public void testPostMovieReturnsForbidden() throws Exception {
        mockMvc.perform(post("/movies/1")).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Pruebas para GET /movies/sessions/{id}
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que consultar una sesión futura devuelve HTTP 200 OK */
    @Test
    public void testFindSessionByIdReturnsOk() throws Exception {
        mockMvc.perform(get("/movies/sessions/" + sessionToday.getId())).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la sesión devuelve el número correcto de asientos libres */
    @Test
    public void testFindSessionByIdReturnsFreeSeats() throws Exception {
        mockMvc.perform(get("/movies/sessions/" + sessionToday.getId()))
            .andExpect(jsonPath("$.freeSeats").value(10));
    }

    /* Partición equivalente: Comprobación de que la sesión devuelve el título de la película asociada */
    @Test
    public void testFindSessionByIdReturnsMovieTitle() throws Exception {
        mockMvc.perform(get("/movies/sessions/" + sessionToday.getId()))
            .andExpect(jsonPath("$.movieTitle").value("Avatar"));
    }

    /* Valor frontera / Caso de error: Comprobación de que consultar una sesión iniciada hace 1 segundo devuelve HTTP 403 Forbidden */
    @Test
    public void testFindSessionByIdAlreadyStartedReturnsForbidden() throws Exception {
        Session pastSession = createSession(avatar, room, LocalDateTime.now().minusSeconds(1));

        mockMvc.perform(get("/movies/sessions/" + pastSession.getId())).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que consultar una sesión inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testFindSessionByIdNonExistentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/movies/sessions/" + NON_EXISTENT_ID)).andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Pruebas para GET /movies/billboard/{day}
    // -------------------------------------------------------------------------

    /* Valor frontera: Comprobación de que consultar la cartelera para el día 0 devuelve HTTP 200 OK */
    @Test
    public void testViewBillboardDayZeroReturnsOk() throws Exception {
        mockMvc.perform(get("/movies/billboard/0")).andExpect(status().isOk());
    }

    /* Valor frontera: Comprobación de que el día 0 contiene exactamente 1 entrada de película */
    @Test
    public void testViewBillboardDayZeroReturnsOneEntry() throws Exception {
        mockMvc.perform(get("/movies/billboard/0")).andExpect(jsonPath("$.length()").value(1));
    }

    /* Valor frontera: Comprobación de que el día 6 contiene 2 películas con sesiones programadas */
    @Test
    public void testViewBillboardDaySixReturnsTwoEntries() throws Exception {
        mockMvc.perform(get("/movies/billboard/6")).andExpect(jsonPath("$.length()").value(2));
    }

    /* Partición equivalente: Comprobación de que las películas se ordenan alfabéticamente por título ("Avatar" antes que "Batman") */
    @Test
    public void testViewBillboardDaySixFirstEntryIsAvatar() throws Exception {
        mockMvc.perform(get("/movies/billboard/6")).andExpect(jsonPath("$[0].movieTitle").value("Avatar"));
    }

    /* Partición equivalente: Comprobación de que las 2 sesiones del día 6 para Avatar se agrupan dentro de la misma entrada */
    @Test
    public void testViewBillboardDaySixAvatarGroupsItsTwoSessions() throws Exception {
        mockMvc.perform(get("/movies/billboard/6")).andExpect(jsonPath("$[0].sessions.length()").value(2));
    }

    /* Partición equivalente: Comprobación de que consultar un día sin sesiones (día 3) devuelve una lista vacía */
    @Test
    public void testViewBillboardDayWithoutSessionsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/movies/billboard/3")).andExpect(jsonPath("$.length()").value(0));
    }

    /* Valor frontera / Caso de error: Comprobación de que consultar un offset de día negativo (-1) devuelve HTTP 403 Forbidden */
    @Test
    public void testViewBillboardDayMinusOneReturnsForbidden() throws Exception {
        mockMvc.perform(get("/movies/billboard/-1")).andExpect(status().isForbidden());
    }

    /* Valor frontera / Caso de error: Comprobación de que consultar un offset mayor a 6 (día 7) devuelve HTTP 403 Forbidden */
    @Test
    public void testViewBillboardDaySevenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/movies/billboard/7")).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un parámetro de día no numérico devuelve HTTP 400 Bad Request */
    @Test
    public void testViewBillboardDayNotANumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/movies/billboard/abc")).andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un segmento de ruta adicional no declarado devuelve HTTP 403 Forbidden */
    @Test
    public void testViewBillboardExtraPathSegmentReturnsForbidden() throws Exception {
        mockMvc.perform(get("/movies/billboard/1/extra")).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Pruebas de rendimiento (JETM) de extremo a extremo
    // -------------------------------------------------------------------------

    /* Rendimiento / Requisito no funcional: Comprobación de que el tiempo medio de GET /movies/billboard/{day},
     * con una cartelera de 5 películas y 4 sesiones por película, está por debajo del umbral */
    @Test
    public void testViewBillboardRequestAverageTimeIsBelowThreshold() throws Exception {
        LocalDate day = LocalDate.now().plusDays(2);
        for (int i = 0; i < PERFORMANCE_MOVIES; i++) {
            Movie movie = createMovie("Performance Movie " + i);
            for (int j = 0; j < PERFORMANCE_SESSIONS_PER_MOVIE; j++) {
                createSession(movie, room, day.atTime(10 + 3 * j, 0));
            }
        }
        flushAndClear();

        PerformanceMeter.Result result = PerformanceMeter.measure("GET /movies/billboard/{day}", () -> {
            mockMvc.perform(get("/movies/billboard/2"));
            flushAndClear();
        });

        assertTrue(result.averageMillis() < VIEW_BILLBOARD_REQUEST_THRESHOLD_MS);
    }
}