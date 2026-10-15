package es.udc.paproject.backend.rest.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.User;

public class OrderControllerIntegrationTest extends ControllerIntegrationTestBase {

    private User user;
    private User otherUser;
    private User clerk;
    private Movie movie;
    private Room room;
    private Session session;

    @BeforeEach
    public void setUp() {
        user = createUser("viewer", User.RoleType.USER);
        otherUser = createUser("other", User.RoleType.USER);
        clerk = createUser("clerk", User.RoleType.CLERK);

        movie = createMovie("Avatar");
        room = createRoom(10);
        session = createSession(movie, room, LocalDate.now().plusDays(1).atTime(12, 0));
    }

    private ResultActions buy(String authorization, Long sessionId, int numTickets, String creditCardNum) throws Exception {
        String body = """
            {"sessionId":%d,"numTickets":%d,"creditCardNum":"%s"}
            """.formatted(sessionId, numTickets, creditCardNum);

        return mockMvc.perform(post("/orders").contentType(APPLICATION_JSON).content(body)
            .header(AUTHORIZATION, authorization));
    }

    private ResultActions deliver(String authorization, Long orderId, String creditCardNum) throws Exception {
        String body = """
            {"creditCardNum":"%s"}
            """.formatted(creditCardNum);

        return mockMvc.perform(post("/orders/" + orderId + "/deliver").contentType(APPLICATION_JSON).content(body)
            .header(AUTHORIZATION, authorization));
    }

    private Order createOrder(User owner, Session orderSession, LocalDateTime date) {
        return orderDao.save(new Order(2, orderSession, CREDIT_CARD, date, owner));
    }

    // -------------------------------------------------------------------------
    // Pruebas para POST /orders
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que la compra correcta devuelve HTTP 200 OK */
    @Test
    public void testBuyTicketsSuccessReturnsOk() throws Exception {
        buy(bearer(user), session.getId(), 2, CREDIT_CARD).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la compra devuelve el ID del pedido */
    @Test
    public void testBuyTicketsSuccessReturnsOrderId() throws Exception {
        buy(bearer(user), session.getId(), 2, CREDIT_CARD).andExpect(jsonPath("$").isNumber());
    }

    /* Partición equivalente: Comprobación de que la compra reduce los asientos libres en la BD */
    @Test
    public void testBuyTicketsReducesFreeSeats() throws Exception {
        buy(bearer(user), session.getId(), 2, CREDIT_CARD);
        flushAndClear();

        assertEquals(8, sessionDao.findById(session.getId()).get().getFreeSeats());
    }

    /* Partición equivalente: Comprobación de que la compra se guarda a nombre del usuario autenticado */
    @Test
    public void testBuyTicketsOrderBelongsToTokenUser() throws Exception {
        buy(bearer(user), session.getId(), 2, CREDIT_CARD);
        flushAndClear();

        assertEquals(user.getId(), orderDao.findByUserIdOrderByDateDesc(user.getId(),
            PageRequest.of(0, 10)).getContent().get(0).getUser().getId());
    }

    /* Partición equivalente / Caso de error: Comprobación de que comprar sin autenticación devuelve HTTP 403 Forbidden */
    @Test
    public void testBuyTicketsAnonymousReturnsForbidden() throws Exception {
        mockMvc.perform(post("/orders").contentType(APPLICATION_JSON)
            .content("{\"sessionId\":" + session.getId() + ",\"numTickets\":2,\"creditCardNum\":\"" + CREDIT_CARD + "\"}"))
            .andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un taquillero (CLERK) no puede comprar entradas (HTTP 403) */
    @Test
    public void testBuyTicketsAsClerkReturnsForbidden() throws Exception {
        buy(bearer(clerk), session.getId(), 2, CREDIT_CARD).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un token inválido devuelve HTTP 401 Unauthorized */
    @Test
    public void testBuyTicketsInvalidTokenReturnsUnauthorized() throws Exception {
        buy(INVALID_TOKEN_HEADER, session.getId(), 2, CREDIT_CARD).andExpect(status().isUnauthorized());
    }

    /* Valor frontera / Caso de error: Comprobación de que comprar 0 entradas devuelve HTTP 400 Bad Request */
    @Test
    public void testBuyTicketsZeroTicketsReturnsBadRequest() throws Exception {
        buy(bearer(user), session.getId(), 0, CREDIT_CARD).andExpect(status().isBadRequest());
    }

    /* Valor frontera: Comprobación de que se permite comprar 1 entrada */
    @Test
    public void testBuyTicketsOneTicketReturnsOk() throws Exception {
        buy(bearer(user), session.getId(), 1, CREDIT_CARD).andExpect(status().isOk());
    }

    /* Valor frontera: Comprobación de que se permite comprar 10 entradas (capacidad total) */
    @Test
    public void testBuyTicketsTenTicketsReturnsOk() throws Exception {
        buy(bearer(user), session.getId(), 10, CREDIT_CARD).andExpect(status().isOk());
    }

    /* Valor frontera / Caso de error: Comprobación de que comprar 11 entradas devuelve HTTP 400 Bad Request */
    @Test
    public void testBuyTicketsElevenTicketsReturnsBadRequest() throws Exception {
        buy(bearer(user), session.getId(), 11, CREDIT_CARD).andExpect(status().isBadRequest());
    }

    /* Valor frontera / Caso de error: Comprobación de que una tarjeta vacía devuelve HTTP 400 Bad Request */
    @Test
    public void testBuyTicketsEmptyCreditCardReturnsBadRequest() throws Exception {
        buy(bearer(user), session.getId(), 2, "").andExpect(status().isBadRequest());
    }

    /* Valor frontera: Comprobación de que se permite una tarjeta de 1 carácter */
    @Test
    public void testBuyTicketsCreditCardOfOneCharacterReturnsOk() throws Exception {
        buy(bearer(user), session.getId(), 2, "1").andExpect(status().isOk());
    }

    /* Valor frontera: Comprobación de que se permite una tarjeta de 16 caracteres */
    @Test
    public void testBuyTicketsCreditCardOfSixteenCharactersReturnsOk() throws Exception {
        buy(bearer(user), session.getId(), 2, "1234567890123456").andExpect(status().isOk());
    }

    /* Valor frontera / Caso de error: Comprobación de que una tarjeta de 17 caracteres devuelve HTTP 400 Bad Request */
    @Test
    public void testBuyTicketsCreditCardOfSeventeenCharactersReturnsBadRequest() throws Exception {
        buy(bearer(user), session.getId(), 2, "12345678901234567").andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Caso de error: Comprobación de que omitir el sessionId devuelve HTTP 400 Bad Request */
    @Test
    public void testBuyTicketsMissingSessionIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/orders").contentType(APPLICATION_JSON).header(AUTHORIZATION, bearer(user))
            .content("{\"numTickets\":2,\"creditCardNum\":\"" + CREDIT_CARD + "\"}")).andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Caso de error: Comprobación de que omitir el numTickets devuelve HTTP 400 Bad Request */
    @Test
    public void testBuyTicketsMissingNumTicketsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/orders").contentType(APPLICATION_JSON).header(AUTHORIZATION, bearer(user))
            .content("{\"sessionId\":" + session.getId() + ",\"creditCardNum\":\"" + CREDIT_CARD + "\"}"))
            .andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Caso de error: Comprobación de que comprar para una sesión inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testBuyTicketsNonExistentSessionReturnsNotFound() throws Exception {
        buy(bearer(user), NON_EXISTENT_ID, 2, CREDIT_CARD).andExpect(status().isNotFound());
    }

    /* Valor frontera / Caso de error: Comprobación de que comprar para una sesión ya comenzada devuelve HTTP 403 Forbidden */
    @Test
    public void testBuyTicketsSessionAlreadyStartedReturnsForbidden() throws Exception {
        Session pastSession = createSession(movie, room, LocalDateTime.now().minusSeconds(1));

        buy(bearer(user), pastSession.getId(), 2, CREDIT_CARD).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que solicitar más entradas de las restantes devuelve HTTP 403 Forbidden */
    @Test
    public void testBuyTicketsNotEnoughTicketsLeftReturnsForbidden() throws Exception {
        buy(bearer(user), session.getId(), 6, CREDIT_CARD);

        buy(bearer(otherUser), session.getId(), 5, CREDIT_CARD).andExpect(status().isForbidden());
    }

    /* Valor frontera: Comprobación de que se permite comprar exactamente las plazas restantes */
    @Test
    public void testBuyTicketsExactlyRemainingTicketsReturnsOk() throws Exception {
        buy(bearer(user), session.getId(), 6, CREDIT_CARD);

        buy(bearer(otherUser), session.getId(), 4, CREDIT_CARD).andExpect(status().isOk());
    }

    /* Partición equivalente / Caso de error: Comprobación de que comprar con un usuario inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testBuyTicketsNonExistentUserReturnsNotFound() throws Exception {
        buy(bearerFor(NON_EXISTENT_ID, "USER"), session.getId(), 2, CREDIT_CARD).andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Pruebas para GET /orders
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que un usuario sin compras recibe una lista vacía */
    @Test
    public void testFindUserOrdersWithoutOrdersReturnsEmptyItems() throws Exception {
        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(user))).andExpect(jsonPath("$.items").isEmpty());
    }

    /* Partición equivalente: Comprobación de que un usuario sin compras no tiene más páginas */
    @Test
    public void testFindUserOrdersWithoutOrdersHasNoMoreItems() throws Exception {
        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(user)))
            .andExpect(jsonPath("$.existMoreItems").value(false));
    }

    /* Partición equivalente: Comprobación de que no se devuelven las compras de otros usuarios */
    @Test
    public void testFindUserOrdersDoesNotReturnOtherUsersOrders() throws Exception {
        createOrder(otherUser, session, LocalDateTime.now().minusMinutes(10));

        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(user))).andExpect(jsonPath("$.items").isEmpty());
    }

    /* Valor frontera: Comprobación de que la primera página respeta el tamaño de elementos */
    @Test
    public void testFindUserOrdersFirstPageHasTwoItems() throws Exception {
        createThreeOrders();

        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(user))).andExpect(jsonPath("$.items.length()").value(2));
    }

    /* Valor frontera: Comprobación de que existMoreItems es true cuando hay más páginas */
    @Test
    public void testFindUserOrdersFirstPageHasMoreItems() throws Exception {
        createThreeOrders();

        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(user)))
            .andExpect(jsonPath("$.existMoreItems").value(true));
    }

    /* Partición equivalente: Comprobación de que los elementos se ordenan por fecha descendente */
    @Test
    public void testFindUserOrdersFirstItemIsTheMostRecent() throws Exception {
        Order[] orders = createThreeOrders();

        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(user)))
            .andExpect(jsonPath("$.items[0].id").value(orders[2].getId().intValue()));
    }

    /* Valor frontera: Comprobación de la recuperación de la segunda página */
    @Test
    public void testFindUserOrdersSecondPageHasOneItem() throws Exception {
        createThreeOrders();

        mockMvc.perform(get("/orders").param("page", "1").header(AUTHORIZATION, bearer(user)))
            .andExpect(jsonPath("$.items.length()").value(1));
    }

    /* Valor frontera: Comprobación de que la última página indica existMoreItems = false */
    @Test
    public void testFindUserOrdersSecondPageHasNoMoreItems() throws Exception {
        createThreeOrders();

        mockMvc.perform(get("/orders").param("page", "1").header(AUTHORIZATION, bearer(user)))
            .andExpect(jsonPath("$.existMoreItems").value(false));
    }

    /* Partición equivalente / Caso de error: Comprobación de que consultar pedidos sin autenticación devuelve HTTP 403 Forbidden */
    @Test
    public void testFindUserOrdersAnonymousReturnsForbidden() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un taquillero (CLERK) no puede consultar pedidos (HTTP 403) */
    @Test
    public void testFindUserOrdersAsClerkReturnsForbidden() throws Exception {
        mockMvc.perform(get("/orders").header(AUTHORIZATION, bearer(clerk))).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un token inválido devuelve HTTP 401 Unauthorized */
    @Test
    public void testFindUserOrdersInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/orders").header(AUTHORIZATION, INVALID_TOKEN_HEADER))
            .andExpect(status().isUnauthorized());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un parámetro page no numérico devuelve HTTP 400 Bad Request */
    @Test
    public void testFindUserOrdersPageNotANumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/orders").param("page", "abc").header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isBadRequest());
    }

    /* Valor frontera / Bug detectado (Issue #4): Comprobación de que un número de página negativo (-1) devuelve HTTP 400 Bad Request */
    @Test
    public void testFindUserOrdersNegativePageReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/orders").param("page", "-1").header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isBadRequest());
    }

    private Order[] createThreeOrders() {
        Order oldest = createOrder(user, session, LocalDateTime.now().minusMinutes(30));
        Order middle = createOrder(user, session, LocalDateTime.now().minusMinutes(20));
        Order newest = createOrder(user, session, LocalDateTime.now().minusMinutes(10));

        return new Order[] {oldest, middle, newest};
    }

    // -------------------------------------------------------------------------
    // Pruebas para POST /orders/{id}/deliver
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que la entrega correcta de entradas por un CLERK devuelve HTTP 200 OK */
    @Test
    public void testDeliverTicketsSuccessReturnsOk() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(clerk), order.getId(), CREDIT_CARD).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la entrega actualiza el estado delivered en la BD */
    @Test
    public void testDeliverTicketsMarksOrderAsDelivered() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(clerk), order.getId(), CREDIT_CARD);
        flushAndClear();

        assertTrue(orderDao.findById(order.getId()).get().isDelivered());
    }

    /* Partición equivalente / Caso de error: Comprobación de que una tarjeta incorrecta devuelve HTTP 403 Forbidden */
    @Test
    public void testDeliverTicketsWrongCreditCardReturnsForbidden() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(clerk), order.getId(), "8765432187654321").andExpect(status().isForbidden());
    }

    /* Valor frontera / Caso de error: Comprobación de que una tarjeta que falla solo en el último dígito devuelve HTTP 403 Forbidden */
    @Test
    public void testDeliverTicketsCreditCardDifferingInLastDigitReturnsForbidden() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(clerk), order.getId(), "1234567812345679").andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que entregar un pedido ya entregado devuelve HTTP 403 Forbidden */
    @Test
    public void testDeliverTicketsAlreadyDeliveredReturnsForbidden() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());
        deliver(bearer(clerk), order.getId(), CREDIT_CARD);

        deliver(bearer(clerk), order.getId(), CREDIT_CARD).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que entregar un pedido inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testDeliverTicketsNonExistentOrderReturnsNotFound() throws Exception {
        deliver(bearer(clerk), NON_EXISTENT_ID, CREDIT_CARD).andExpect(status().isNotFound());
    }

    /* Valor frontera / Caso de error: Comprobación de que entregar entradas de una sesión comenzada hace 1 segundo devuelve HTTP 403 Forbidden */
    @Test
    public void testDeliverTicketsSessionAlreadyStartedReturnsForbidden() throws Exception {
        Session pastSession = createSession(movie, room, LocalDateTime.now().minusSeconds(1));
        Order order = createOrder(user, pastSession, LocalDateTime.now().minusHours(1));

        deliver(bearer(clerk), order.getId(), CREDIT_CARD).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un usuario normal (USER) no puede entregar entradas (HTTP 403) */
    @Test
    public void testDeliverTicketsAsUserReturnsForbidden() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(user), order.getId(), CREDIT_CARD).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que entregar sin autenticación devuelve HTTP 403 Forbidden */
    @Test
    public void testDeliverTicketsAnonymousReturnsForbidden() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        mockMvc.perform(post("/orders/" + order.getId() + "/deliver").contentType(APPLICATION_JSON)
            .content("{\"creditCardNum\":\"" + CREDIT_CARD + "\"}")).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un token inválido devuelve HTTP 401 Unauthorized */
    @Test
    public void testDeliverTicketsInvalidTokenReturnsUnauthorized() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(INVALID_TOKEN_HEADER, order.getId(), CREDIT_CARD).andExpect(status().isUnauthorized());
    }

    /* Valor frontera / Caso de error: Comprobación de que entregar con tarjeta vacía devuelve HTTP 400 Bad Request */
    @Test
    public void testDeliverTicketsEmptyCreditCardReturnsBadRequest() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(clerk), order.getId(), "").andExpect(status().isBadRequest());
    }

    /* Valor frontera / Caso de error: Comprobación de que entregar con tarjeta de 17 caracteres devuelve HTTP 400 Bad Request */
    @Test
    public void testDeliverTicketsCreditCardOfSeventeenCharactersReturnsBadRequest() throws Exception {
        Order order = createOrder(user, session, LocalDateTime.now());

        deliver(bearer(clerk), order.getId(), "12345678901234567").andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Flujo completo y rutas no declaradas
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación del flujo completo de compra por usuario y entrega por taquillero */
    @Test
    public void testBuyThenDeliverFlowMarksOrderAsDelivered() throws Exception {
        buy(bearer(user), session.getId(), 2, CREDIT_CARD);
        flushAndClear();
        Long orderId = orderDao.findByUserIdOrderByDateDesc(user.getId(),
            PageRequest.of(0, 1)).getContent().get(0).getId();

        deliver(bearer(clerk), orderId, CREDIT_CARD);
        flushAndClear();

        assertTrue(orderDao.findById(orderId).get().isDelivered());
    }

    /* Partición equivalente: Comprobación de que GET sobre la ruta de entrega devuelve HTTP 403 Forbidden */
    @Test
    public void testGetDeliverRouteReturnsForbidden() throws Exception {
        mockMvc.perform(get("/orders/1/deliver").header(AUTHORIZATION, bearer(clerk))).andExpect(status().isForbidden());
    }

    /* Partición equivalente: Comprobación de que DELETE sobre /orders/{id} devuelve HTTP 403 Forbidden */
    @Test
    public void testDeleteOrderReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/orders/1").header(AUTHORIZATION, bearer(user))).andExpect(status().isForbidden());
    }
}