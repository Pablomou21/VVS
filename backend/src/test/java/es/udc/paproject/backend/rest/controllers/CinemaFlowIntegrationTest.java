package es.udc.paproject.backend.rest.controllers;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.User;

/**
 * Pruebas de integración de extremo a extremo que encadenan varias operaciones de la API REST tal y como lo haría
 * un cliente real: alta, inicio de sesión, compra, consulta del historial y entrega en taquilla.
 *
 * A diferencia del resto de pruebas REST, los tokens JWT no se generan con JwtGenerator, sino que se obtienen de las
 * respuestas de la propia API (signUp y login), de modo que se comprueba también que lo que devuelve una operación
 * sirve como entrada de la siguiente.
 */
public class CinemaFlowIntegrationTest extends ControllerIntegrationTestBase {

    private static final String CLERK_USER_NAME = "clerk";
    private static final int SESSION_CAPACITY = 10;
    private static final int TICKETS = 3;

    @Autowired
    private ObjectMapper objectMapper;

    private Session session;

    @BeforeEach
    public void setUp() {
        // El alta pública siempre crea usuarios USER, así que el taquillero se da de alta directamente en la BD
        createUser(CLERK_USER_NAME, User.RoleType.CLERK);
        session = createSession(createMovie("Avatar"), createRoom(SESSION_CAPACITY),
            LocalDate.now().plusDays(1).atTime(20, 0));
    }

    // -------------------------------------------------------------------------
    // Pasos del flujo (llamadas reales a la API)
    // -------------------------------------------------------------------------

    private ResultActions performSignUp(String userName) throws Exception {
        String body = """
            {"userName":"%s","password":"%s","firstName":"Name","lastName":"LastName","email":"%s@test.com"}
            """.formatted(userName, PASSWORD, userName);

        return mockMvc.perform(post("/users/signUp").contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions performLogin(String userName, String password) throws Exception {
        String body = """
            {"userName":"%s","password":"%s"}
            """.formatted(userName, password);

        return mockMvc.perform(post("/users/login").contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions performBuy(String token, int numTickets) throws Exception {
        String body = """
            {"sessionId":%d,"numTickets":%d,"creditCardNum":"%s"}
            """.formatted(session.getId(), numTickets, CREDIT_CARD);

        return mockMvc.perform(post("/orders").contentType(APPLICATION_JSON).content(body)
            .header(AUTHORIZATION, bearer(token)));
    }

    private ResultActions performFindOrders(String token) throws Exception {
        return mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(token)));
    }

    private ResultActions performDeliver(String token, Long orderId) throws Exception {
        String body = """
            {"creditCardNum":"%s"}
            """.formatted(CREDIT_CARD);

        return mockMvc.perform(post("/orders/" + orderId + "/deliver").contentType(APPLICATION_JSON).content(body)
            .header(AUTHORIZATION, bearer(token)));
    }

    private ResultActions performChangePassword(String token, Long userId, String oldPassword, String newPassword)
            throws Exception {
        String body = """
            {"oldPassword":"%s","newPassword":"%s"}
            """.formatted(oldPassword, newPassword);

        return mockMvc.perform(post("/users/" + userId + "/changePassword").contentType(APPLICATION_JSON)
            .content(body).header(AUTHORIZATION, bearer(token)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // -------------------------------------------------------------------------
    // Extracción de datos de las respuestas (para encadenar operaciones)
    // -------------------------------------------------------------------------

    private String tokenOf(ResultActions response) throws Exception {
        return objectMapper.readTree(content(response)).get("serviceToken").asText();
    }

    private Long userIdOf(ResultActions response) throws Exception {
        return objectMapper.readTree(content(response)).get("user").get("id").asLong();
    }

    private Long orderIdOf(ResultActions response) throws Exception {
        return Long.valueOf(content(response));
    }

    private String content(ResultActions response) throws Exception {
        MvcResult result = response.andReturn();
        return result.getResponse().getContentAsString();
    }

    // -------------------------------------------------------------------------
    // Flujos compuestos
    // -------------------------------------------------------------------------

    private String signUpAndGetToken(String userName) throws Exception {
        return tokenOf(performSignUp(userName));
    }

    private String clerkLoginToken() throws Exception {
        return tokenOf(performLogin(CLERK_USER_NAME, PASSWORD));
    }

    /* Alta de un espectador y compra de entradas con el token del alta; devuelve el token del espectador */
    private String signUpAndBuy(String userName) throws Exception {
        String token = signUpAndGetToken(userName);
        performBuy(token, TICKETS);
        return token;
    }

    /* Alta, compra y entrega en taquilla con el token del login del taquillero; devuelve el token del espectador */
    private String signUpBuyAndDeliver(String userName) throws Exception {
        String token = signUpAndGetToken(userName);
        Long orderId = orderIdOf(performBuy(token, TICKETS));
        performDeliver(clerkLoginToken(), orderId);
        return token;
    }

    // -------------------------------------------------------------------------
    // Alta e inicio de sesión: los tokens devueltos por la API son utilizables
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que el token devuelto por el alta permite comprar entradas (HTTP 200 OK) */
    @Test
    public void testSignUpTokenAllowsBuyingTickets() throws Exception {
        String token = signUpAndGetToken("viewer");

        performBuy(token, TICKETS).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que el token devuelto por el login de un usuario recién dado de alta
     * permite comprar entradas (HTTP 200 OK) */
    @Test
    public void testLoginTokenAfterSignUpAllowsBuyingTickets() throws Exception {
        performSignUp("viewer");
        String token = tokenOf(performLogin("viewer", PASSWORD));

        performBuy(token, TICKETS).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que el token del alta permite recuperar la sesión con
     * loginFromServiceToken y devuelve el mismo usuario */
    @Test
    public void testSignUpTokenAllowsLoginFromServiceToken() throws Exception {
        String token = signUpAndGetToken("viewer");

        mockMvc.perform(post("/users/loginFromServiceToken").header(AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$.user.userName").value("viewer"));
    }

    /* Partición equivalente / Caso de error: Comprobación de que el token de un espectador obtenido en el alta
     * no permite entregar entradas en taquilla (HTTP 403 Forbidden) */
    @Test
    public void testSignUpTokenCannotDeliverTickets() throws Exception {
        String token = signUpAndGetToken("viewer");
        Long orderId = orderIdOf(performBuy(token, TICKETS));

        performDeliver(token, orderId).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que el token del login del taquillero no permite
     * comprar entradas (HTTP 403 Forbidden) */
    @Test
    public void testClerkLoginTokenCannotBuyTickets() throws Exception {
        performBuy(clerkLoginToken(), TICKETS).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Alta + compra + historial
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que, tras el alta y una compra, el historial contiene 1 compra */
    @Test
    public void testFlowOrderHistoryAfterPurchaseHasOneItem() throws Exception {
        String token = signUpAndBuy("viewer");

        performFindOrders(token).andExpect(jsonPath("$.items.length()").value(1));
    }

    /* Partición equivalente: Comprobación de que la compra del historial refleja el número de entradas compradas */
    @Test
    public void testFlowOrderHistoryShowsPurchasedUnits() throws Exception {
        String token = signUpAndBuy("viewer");

        performFindOrders(token).andExpect(jsonPath("$.items[0].units").value(TICKETS));
    }

    /* Partición equivalente: Comprobación de que la compra del historial refleja el título de la película */
    @Test
    public void testFlowOrderHistoryShowsMovieTitle() throws Exception {
        String token = signUpAndBuy("viewer");

        performFindOrders(token).andExpect(jsonPath("$.items[0].movieTitle").value("Avatar"));
    }

    /* Partición equivalente: Comprobación de que el identificador devuelto por la compra coincide con el de la
     * compra que aparece en el historial */
    @Test
    public void testFlowOrderHistoryShowsReturnedOrderId() throws Exception {
        String token = signUpAndGetToken("viewer");
        Long orderId = orderIdOf(performBuy(token, TICKETS));

        performFindOrders(token).andExpect(jsonPath("$.items[0].id").value(orderId));
    }

    /* Partición equivalente: Comprobación de que una compra recién hecha aparece como no entregada */
    @Test
    public void testFlowOrderHistoryShowsNotDeliveredBeforeDelivery() throws Exception {
        String token = signUpAndBuy("viewer");

        performFindOrders(token).andExpect(jsonPath("$.items[0].delivered").value(false));
    }

    /* Partición equivalente: Comprobación de que las compras de un espectador no aparecen en el historial de otro
     * espectador dado de alta después */
    @Test
    public void testFlowOrderHistoryOfAnotherUserIsEmpty() throws Exception {
        signUpAndBuy("viewer");
        String otherToken = signUpAndGetToken("other");

        performFindOrders(otherToken).andExpect(jsonPath("$.items.length()").value(0));
    }

    /* Valor frontera: Comprobación de que, tras comprar 3 entradas en una sala de 10, la sesión muestra 7 plazas
     * libres al consultarla por la API */
    @Test
    public void testFlowFreeSeatsDecreaseAfterPurchase() throws Exception {
        signUpAndBuy("viewer");

        mockMvc.perform(get("/movies/sessions/" + session.getId()))
            .andExpect(jsonPath("$.freeSeats").value(SESSION_CAPACITY - TICKETS));
    }

    /* Valor frontera / Caso de error: Comprobación de que, tras agotar las plazas con dos espectadores distintos,
     * una tercera compra de 1 entrada se rechaza (HTTP 403 Forbidden) */
    @Test
    public void testFlowPurchaseAfterSeatsExhaustedByOtherUsersReturnsForbidden() throws Exception {
        performBuy(signUpAndGetToken("viewer1"), 6);
        performBuy(signUpAndGetToken("viewer2"), 4);

        performBuy(signUpAndGetToken("viewer3"), 1).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Alta + compra + entrega en taquilla
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que el taquillero, con el token de su login, puede entregar la compra
     * hecha por un espectador con el token de su alta (HTTP 200 OK) */
    @Test
    public void testFlowClerkLoginTokenAllowsDeliveringTickets() throws Exception {
        String token = signUpAndGetToken("viewer");
        Long orderId = orderIdOf(performBuy(token, TICKETS));

        performDeliver(clerkLoginToken(), orderId).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que, tras la entrega, la compra aparece como entregada en el historial
     * del espectador */
    @Test
    public void testFlowOrderHistoryShowsDeliveredAfterDelivery() throws Exception {
        String token = signUpBuyAndDeliver("viewer");

        performFindOrders(token).andExpect(jsonPath("$.items[0].delivered").value(true));
    }

    /* Caso de error: Comprobación de que entregar por segunda vez la misma compra se rechaza (HTTP 403 Forbidden) */
    @Test
    public void testFlowSecondDeliveryReturnsForbidden() throws Exception {
        String token = signUpAndGetToken("viewer");
        Long orderId = orderIdOf(performBuy(token, TICKETS));
        String clerkToken = clerkLoginToken();
        performDeliver(clerkToken, orderId);

        performDeliver(clerkToken, orderId).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Alta + cambio de contraseña + inicio de sesión
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que, tras cambiar la contraseña con el token del alta, se puede iniciar
     * sesión con la nueva (HTTP 200 OK) */
    @Test
    public void testFlowLoginWithNewPasswordAfterChangeReturnsOk() throws Exception {
        ResultActions signUp = performSignUp("viewer");
        performChangePassword(tokenOf(signUp), userIdOf(signUp), PASSWORD, "newPassword");

        performLogin("viewer", "newPassword").andExpect(status().isOk());
    }

    /* Caso de error: Comprobación de que, tras cambiar la contraseña con el token del alta, la contraseña antigua
     * ya no permite iniciar sesión (HTTP 404 Not Found) */
    @Test
    public void testFlowLoginWithOldPasswordAfterChangeReturnsNotFound() throws Exception {
        ResultActions signUp = performSignUp("viewer");
        performChangePassword(tokenOf(signUp), userIdOf(signUp), PASSWORD, "newPassword");

        performLogin("viewer", PASSWORD).andExpect(status().isNotFound());
    }

    /* Partición equivalente: Comprobación de que el token obtenido antes de cambiar la contraseña sigue permitiendo
     * comprar entradas (el token no depende de la contraseña) */
    @Test
    public void testFlowTokenFromBeforePasswordChangeStillAllowsBuying() throws Exception {
        ResultActions signUp = performSignUp("viewer");
        String token = tokenOf(signUp);
        performChangePassword(token, userIdOf(signUp), PASSWORD, "newPassword");

        performBuy(token, TICKETS).andExpect(status().isOk());
    }
}
