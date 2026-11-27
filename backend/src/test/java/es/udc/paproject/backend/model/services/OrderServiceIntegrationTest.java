package es.udc.paproject.backend.model.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.entities.OrderDao;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.RoomDao;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.entities.UserDao;
import es.udc.paproject.backend.model.exceptions.AlreadyDeliveredException;
import es.udc.paproject.backend.model.exceptions.CreditCardException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.NotEnoughTicketsException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import es.udc.paproject.backend.testutil.PerformanceMeter;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceIntegrationTest {

    private static final Long NON_EXISTENT_ID = -1L;

    /* Criterio común de los umbrales de servicio contra la BD real: 100 ms es el límite para que el usuario perciba
     * una respuesta como instantánea (J. Nielsen, "Response Times: The 3 Important Limits"). Se reserva la mitad
     * (50 ms) para la operación de servicio con la BD y el resto para la red, la capa REST y el frontend. */

    /* Requisito no funcional: una compra de entradas debe completarse de media en menos de 50 ms contra la BD real
     * (incluye la lectura del usuario y de la sesión, la actualización de las plazas libres y la inserción de la compra).
     * Medición de referencia en desarrollo: ~5 ms de media */
    private static final double BUY_TICKETS_THRESHOLD_MS = 50.0;

    /* Requisito no funcional: la consulta de una página del historial de compras debe responder de media en menos de 50 ms
     * contra la BD real. Medición de referencia en desarrollo: ~4-5 ms de media */
    private static final double FIND_USER_ORDERS_THRESHOLD_MS = 50.0;

    /* Capacidad de la sala de las pruebas de rendimiento: suficiente para el calentamiento y todas las compras medidas */
    private static final int PERFORMANCE_ROOM_CAPACITY = 500;

    /* Número de compras previas del historial en la prueba de rendimiento de findUserOrders */
    private static final int PERFORMANCE_ORDER_HISTORY_SIZE = 30;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private RoomDao roomDao;

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Session session;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @BeforeEach
    public void setUp() {
        user = new User("integrationUser", "password", "Integration", "User", "integration@test.com");
        user.setRole(User.RoleType.USER);
        userDao.save(user);

        Movie movie = new Movie("Test Movie", "Summary", (short) 120);
        movieDao.save(movie);

        Room room = new Room("Room 1", 10);
        roomDao.save(room);

        session = new Session(movie, room, LocalDateTime.now().plusDays(1), new BigDecimal("8.50"));
        sessionDao.save(session);
    }

    // -------------------------------------------------------------------------
    // Pruebas para buyTickets
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que la compra se procesa y se le asigna un ID no nulo */
    @Test
    public void testBuyTicketsSuccess() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        assertNotNull(order.getId());
    }

    /* Partición equivalente: Comprobación de que los asientos libres se reducen en la BD real */
    @Test
    public void testBuyTicketsUpdatesFreeSeats() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");
        flushAndClear();

        Session updatedSession = sessionDao.findById(session.getId()).get();
        assertEquals(8, updatedSession.getFreeSeats());
    }

    /* Partición equivalente / Caso de error: Comprobación de que se lanza InstanceNotFoundException al comprar para un usuario inexistente */
    @Test
    public void testBuyTicketsNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.buyTickets(NON_EXISTENT_ID, session.getId(), 2, "1234567812345678")
        );
    }

    /* Partición equivalente / Caso de error: Comprobación de que se lanza InstanceNotFoundException al comprar para una sesión inexistente */
    @Test
    public void testBuyTicketsNonExistentSession() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.buyTickets(user.getId(), NON_EXISTENT_ID, 2, "1234567812345678")
        );
    }

    /* Valor frontera / Caso de error: Comprobación de que se lanza SessionAlreadyStartedException al comprar para una sesión que comenzó hace solo 1 segundo */
    @Test
    public void testBuyTicketsSessionAlreadyStarted() {
        Session pastSession = new Session(session.getMovie(), session.getRoom(),
                LocalDateTime.now().minusSeconds(1), new BigDecimal("8.50"));
        sessionDao.save(pastSession);

        assertThrows(SessionAlreadyStartedException.class, () ->
            orderService.buyTickets(user.getId(), pastSession.getId(), 2, "1234567812345678")
        );
    }

    /* Valor frontera / Caso de error: Comprobación de que se lanza NotEnoughTicketsException al intentar comprar más entradas de las disponibles */
    @Test
    public void testBuyTicketsNotEnoughTickets() {
        assertThrows(NotEnoughTicketsException.class, () ->
            orderService.buyTickets(user.getId(), session.getId(), 11, "1234567812345678")
        );
    }

    /* Valor frontera: Comprobación de que se puede comprar la capacidad exacta de la sala actualizando a 0 los asientos libres */
    @Test
    public void testBuyTicketsExactCapacity() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 10, "1234567812345678");
        flushAndClear();

        assertEquals(0, sessionDao.findById(session.getId()).get().getFreeSeats());
    }

    /* Valor frontera / Bug detectado (Issue #13): Comprobación de que se lanza IllegalArgumentException al intentar comprar entradas con un número no positivo */
    @Test
    public void testBuyTicketsWithNonPositiveTickets() {
        assertThrows(IllegalArgumentException.class, () ->
            orderService.buyTickets(user.getId(), session.getId(), -2, "1234567812345678")
        );
    }

    /* Valor frontera / Bug detectado (Issue #13): Comprobación de que se lanza IllegalArgumentException al intentar comprar 0 entradas */
    @Test
    public void testBuyTicketsWithZeroTickets() {
        assertThrows(IllegalArgumentException.class, () ->
            orderService.buyTickets(user.getId(), session.getId(), 0, "1234567812345678")
        );
    }

    /* Partición equivalente / Valor frontera: Comprobación de que dos compras sucesivas que suman la capacidad de la sala dejan 0 asientos libres */
    @Test
    public void testBuyTicketsSuccessiveBuysExhaustFreeSeats() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 6, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 4, "1234567812345678");
        flushAndClear();

        assertEquals(0, sessionDao.findById(session.getId()).get().getFreeSeats());
    }

    /* Valor frontera / Caso de error: Comprobación de que, tras agotar las plazas con dos compras, una tercera compra de 1 entrada lanza NotEnoughTicketsException */
    @Test
    public void testBuyTicketsAfterSeatsExhaustedThrowsNotEnoughTickets() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 6, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 4, "1234567812345678");

        assertThrows(NotEnoughTicketsException.class, () ->
            orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678")
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas para findUserOrders
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que se recuperan correctamente las órdenes de un usuario */
    @Test
    public void testFindUserOrdersSuccess() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 0, 10);

        assertEquals(1, ordersBlock.getItems().size());
    }

    /* Partición equivalente / Valor frontera: Comprobación de la paginación cuando existen más registros por consultar */
    @Test
    public void testFindUserOrdersPaginationHasMoreItems() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 0, 1);

        assertTrue(ordersBlock.getExistMoreItems());
    }

    /* Partición equivalente / Caso de error: Comprobación de que se lanza InstanceNotFoundException al consultar órdenes de un usuario inexistente */
    @Test
    public void testFindUserOrdersNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.findUserOrders(NON_EXISTENT_ID, 0, 10)
        );
    }

    /* Valor frontera / Caso de error: Comprobación de que un número de página negativo (-1) lanza IllegalArgumentException */
    @Test
    public void testFindUserOrdersNegativePage() {
        assertThrows(IllegalArgumentException.class, () ->
            orderService.findUserOrders(user.getId(), -1, 10)
        );
    }

    /* Partición equivalente: Comprobación de que el historial devuelve primero la compra más reciente, aunque se haya guardado antes */
    @Test
    public void testFindUserOrdersReturnsMostRecentFirst() throws Exception {
        Order recentOrder = orderDao.save(new Order(1, session, "1234567812345678", LocalDateTime.now().minusDays(1), user));
        orderDao.save(new Order(1, session, "1234567812345678", LocalDateTime.now().minusDays(2), user));
        flushAndClear();

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 0, 10);

        assertEquals(recentOrder.getId(), ordersBlock.getItems().get(0).getId());
    }

    /* Valor frontera: Comprobación de que la segunda página (última) de un historial de 3 compras con tamaño 2 contiene 1 elemento */
    @Test
    public void testFindUserOrdersSecondPageItemCount() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 1, 2);

        assertEquals(1, ordersBlock.getItems().size());
    }

    /* Valor frontera: Comprobación de que la última página de un historial de 3 compras con tamaño 2 indica que no hay más elementos */
    @Test
    public void testFindUserOrdersLastPageHasNoMoreItems() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 1, 2);

        assertFalse(ordersBlock.getExistMoreItems());
    }

    /* Partición equivalente: Comprobación de que un usuario existente sin compras obtiene un historial vacío */
    @Test
    public void testFindUserOrdersUserWithoutOrdersReturnsEmpty() throws Exception {
        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 0, 10);

        assertTrue(ordersBlock.getItems().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Pruebas para deliverTickets
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que se realiza la entrega correcta de un ticket */
    @Test
    public void testDeliverTicketsSuccess() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        orderService.deliverTickets(order.getId(), "1234567812345678");
        flushAndClear();

        assertTrue(orderDao.findById(order.getId()).get().isDelivered());
    }

    /* Valor frontera / Caso de error: Comprobación de que se lanza CreditCardException al entregar con una tarjeta diferente en el último dígito */
    @Test
    public void testDeliverTicketsIncorrectCreditCard() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        assertThrows(CreditCardException.class, () ->
            orderService.deliverTickets(order.getId(), "1234567812345679")
        );
    }

    /* Valor frontera / Caso de error: Comprobación de que se lanza AlreadyDeliveredException al intentar entregar un ticket ya entregado */
    @Test
    public void testDeliverTicketsAlreadyDelivered() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");
        orderService.deliverTickets(order.getId(), "1234567812345678");

        assertThrows(AlreadyDeliveredException.class, () ->
            orderService.deliverTickets(order.getId(), "1234567812345678")
        );
    }

    /* Partición equivalente / Caso de error: Comprobación de que se lanza InstanceNotFoundException al intentar entregar un pedido inexistente */
    @Test
    public void testDeliverTicketsNonExistentOrder() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.deliverTickets(NON_EXISTENT_ID, "1234567812345678")
        );
    }

    /* Valor frontera / Caso de error: Comprobación de que se lanza SessionAlreadyStartedException al entregar las entradas de una sesión que comenzó hace 1 segundo */
    @Test
    public void testDeliverTicketsSessionAlreadyStarted() {
        Session pastSession = sessionDao.save(new Session(session.getMovie(), session.getRoom(),
                LocalDateTime.now().minusSeconds(1), new BigDecimal("8.50")));
        Order order = orderDao.save(new Order(2, pastSession, "1234567812345678", LocalDateTime.now().minusDays(1), user));

        assertThrows(SessionAlreadyStartedException.class, () ->
            orderService.deliverTickets(order.getId(), "1234567812345678")
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas de rendimiento (JETM) contra la BD real
    // -------------------------------------------------------------------------

    /* Rendimiento / Requisito no funcional: Comprobación de que el tiempo medio de buyTickets contra la BD real,
     * en 50 compras medidas tras 10 de calentamiento, está por debajo del umbral */
    @Test
    public void testBuyTicketsAverageTimeIsBelowThreshold() throws Exception {
        Room bigRoom = roomDao.save(new Room("Big Room", PERFORMANCE_ROOM_CAPACITY));
        Session bigSession = sessionDao.save(new Session(session.getMovie(), bigRoom,
                LocalDateTime.now().plusDays(1), new BigDecimal("8.50")));
        flushAndClear();

        PerformanceMeter.Result result = PerformanceMeter.measure("OrderService:buyTickets", () -> {
            orderService.buyTickets(user.getId(), bigSession.getId(), 1, "1234567812345678");
            flushAndClear();
        });

        assertTrue(result.averageMillis() < BUY_TICKETS_THRESHOLD_MS);
    }

    /* Rendimiento / Requisito no funcional: Comprobación de que el tiempo medio de findUserOrders contra la BD real,
     * con un historial de 30 compras y páginas de 10, está por debajo del umbral */
    @Test
    public void testFindUserOrdersAverageTimeIsBelowThreshold() throws Exception {
        for (int i = 0; i < PERFORMANCE_ORDER_HISTORY_SIZE; i++) {
            orderDao.save(new Order(1, session, "1234567812345678", LocalDateTime.now().minusMinutes(i), user));
        }
        flushAndClear();

        PerformanceMeter.Result result = PerformanceMeter.measure("OrderService:findUserOrders", () -> {
            orderService.findUserOrders(user.getId(), 0, 10);
            flushAndClear();
        });

        assertTrue(result.averageMillis() < FIND_USER_ORDERS_THRESHOLD_MS);
    }
}