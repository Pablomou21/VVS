package es.udc.paproject.backend.model.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceIntegrationTest {

    private static final Long NON_EXISTENT_ID = -1L;

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

    private User user;
    private Session session;

    @BeforeEach
    public void setUp() {
        user = new User("integrationUser", "password", "Integration", "User", "integration@test.com");
        user.setRole(User.RoleType.USER);
        userDao.save(user);

        Movie movie = new Movie("Test Movie", "Summary", (short) 120);
        movieDao.save(movie);

        Room room = new Room("Room 1", 10);
        roomDao.save(room);

        // Sesión para mañá (garante que non empezou)
        session = new Session(movie, room, LocalDateTime.now().plusDays(1), new BigDecimal("8.50"));
        sessionDao.save(session);
    }

    //-------------------------------------------------------------------------
    // Pruebas para buyTickets
    //-------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que la compra se procesa y la base de datos le asigna un ID no nulo */
    @Test
    public void testBuyTicketsSuccess() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        assertNotNull(order.getId());
    }

    /* Partición equivalente: Comprobación de que los asientos libres se reducen en la BD real */
    @Test
    public void testBuyTicketsUpdatesFreeSeats() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        Session updatedSession = sessionDao.findById(session.getId()).get();
        assertEquals(8, updatedSession.getFreeSeats());
    }

    /* Caso de error / ID Inexistente: Comprobación de que se lanza una excepción al intentar comprar entradas para un usuario no existente */
    @Test
    public void testBuyTicketsNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.buyTickets(NON_EXISTENT_ID, session.getId(), 2, "1234567812345678")
        );
    }

    /* Caso de error / ID Inexistente: Comprobación de que se lanza una excepción al intentar comprar entradas para una sesión no existente */
    @Test
    public void testBuyTicketsNonExistentSession() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.buyTickets(user.getId(), NON_EXISTENT_ID, 2, "1234567812345678")
        );
    }

    /* Valor frontera: Comprobación de que se lanza una excepción al intentar comprar entradas para una sesión que ya ha comenzado */
    @Test
    public void testBuyTicketsSessionAlreadyStarted() {
        Session pastSession = new Session(session.getMovie(), session.getRoom(),
                LocalDateTime.now().minusHours(1), new BigDecimal("8.50"));
        sessionDao.save(pastSession);

        assertThrows(SessionAlreadyStartedException.class, () ->
            orderService.buyTickets(user.getId(), pastSession.getId(), 2, "1234567812345678")
        );
    }

    /* Valor frontera: Comprobación de que se lanza una excepción al intentar comprar entradas cuando no hay suficientes disponibles */
    @Test
    public void testBuyTicketsNotEnoughTickets() {
        assertThrows(NotEnoughTicketsException.class, () ->
            orderService.buyTickets(user.getId(), session.getId(), 11, "1234567812345678")
        );
    }

    /* Valor frontera: Comprobación de que se puede comprar el número exacto de entradas disponibles y se actualizan los asientos libres en la BD */
    @Test
    public void testBuyTicketsExactCapacity() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 10, "1234567812345678");

        assertEquals(0, sessionDao.findById(session.getId()).get().getFreeSeats());
    }

    /* Valor frontera / Bug detectado: Comprobación de que se lanza una excepción al intentar comprar entradas con un número no positivo */
    @Test
    public void testBuyTicketsWithNonPositiveTickets() {
        assertThrows(IllegalArgumentException.class, () ->
            orderService.buyTickets(user.getId(), session.getId(), -2, "1234567812345678")
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas para findUserOrders
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que se pueden recuperar correctamente las órdenes de un usuario */
    @Test
    public void testFindUserOrdersSuccess() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 0, 10);

        assertEquals(1, ordersBlock.getItems().size());
    }

    /* Partición equivalente / Valor frontera: Comprobación de que se pueden recuperar correctamente las órdenes de un usuario con paginación */
    @Test
    public void testFindUserOrdersPaginationHasMoreItems() throws Exception {
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");
        orderService.buyTickets(user.getId(), session.getId(), 1, "1234567812345678");

        Block<Order> ordersBlock = orderService.findUserOrders(user.getId(), 0, 1);

        assertTrue(ordersBlock.getExistMoreItems());
    }

    /* Caso de error / ID Inexistente: Comprobación de que se lanza una excepción al intentar recuperar las órdenes de un usuario inexistente */
    @Test
    public void testFindUserOrdersNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.findUserOrders(NON_EXISTENT_ID, 0, 10)
        );
    }

    /* Valor frontera: Comprobación de que se lanza una excepción al intentar recuperar las órdenes de un usuario con un número de página negativo */
    @Test
    public void testFindUserOrdersNegativePage() {
        assertThrows(IllegalArgumentException.class, () ->
            orderService.findUserOrders(user.getId(), -1, 10)
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas para deliverTickets
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que se puede entregar correctamente un ticket */
    @Test
    public void testDeliverTicketsSuccess() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        orderService.deliverTickets(order.getId(), "1234567812345678");

        assertTrue(orderDao.findById(order.getId()).get().isDelivered());
    }

    /* Valor frontera / Datos inválidos: Comprobación de que se lanza una excepción al intentar entregar un ticket con una tarjeta de crédito incorrecta */
    @Test
    public void testDeliverTicketsIncorrectCreditCard() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");

        assertThrows(CreditCardException.class, () ->
            orderService.deliverTickets(order.getId(), "8765432187654321")
        );
    }

    /* Valor frontera: Comprobación de que se lanza una excepción al intentar entregar un ticket que ya ha sido entregado */
    @Test
    public void testDeliverTicketsAlreadyDelivered() throws Exception {
        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567812345678");
        orderService.deliverTickets(order.getId(), "1234567812345678");

        assertThrows(AlreadyDeliveredException.class, () ->
            orderService.deliverTickets(order.getId(), "1234567812345678")
        );
    }

    /* Caso de error / ID Inexistente: Comprobación de que se lanza una excepción al intentar entregar un ticket de un pedido que no existe */
    @Test
    public void testDeliverTicketsNonExistentOrder() {
        assertThrows(InstanceNotFoundException.class, () ->
            orderService.deliverTickets(NON_EXISTENT_ID, "1234567812345678")
        );
    }
}