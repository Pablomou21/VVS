package es.udc.paproject.backend.test.model.services;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.MovieDao;
import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.entities.OrderDao;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.RoomDao;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.exceptions.*;
import es.udc.paproject.backend.model.services.Block;
import es.udc.paproject.backend.model.services.OrderService;
import es.udc.paproject.backend.model.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceTest {

    private final Long NON_EXISTENT_ID = Long.valueOf(-1);

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private RoomDao roomDao;

    @Autowired
    private OrderDao orderDao;



    private Order createOrder(Session session, String creditCardNum, User user) {
        return new Order(3, session, creditCardNum, LocalDateTime.now(), user);
    }

    private User signUpUser(String userName) {

        User user = new User(userName, "password", "firstName", "lastName", userName + "@" + userName + ".com");

        try {
            userService.signUp(user);
        } catch (DuplicateInstanceException e) {
            throw new RuntimeException(e);
        }

        return user;

    }

    private Order createOrder(int units, Session session, String creditCardNum, LocalDateTime date, User user) {
        return new Order(units, session, creditCardNum, date, user);
    }

    private Session createSession(LocalDateTime date) {
        Movie movie = movieDao.save(new Movie("Movie 1", "Description", (short) 120));
        Room room = roomDao.save(new Room("Room 1", 100));
        Session session = new Session(movie, room, date, new BigDecimal("7.50"));
        return sessionDao.save(session);
    }

    @Test
    public void testBuyTickets() throws InstanceNotFoundException, SessionAlreadyStartedException, NotEnoughTicketsException {
        User user = signUpUser("user");
        Session session = createSession(LocalDateTime.now().plusDays(1));
        int initialFreeSeats = session.getFreeSeats();

        Order order = orderService.buyTickets(user.getId(), session.getId(), 2, "1234567890123456");

        assertEquals(user.getId(), order.getUser().getId());
        assertEquals(session.getId(), order.getSession().getId());
        assertEquals(2, order.getUnits());
        assertEquals("1234567890123456", order.getCreditCardNum());
        assertFalse(order.isDelivered());
        assertEquals(initialFreeSeats - 2, session.getFreeSeats());

        // Get order from DB
        Optional<Order> foundOrderOpt = orderDao.findById(order.getId());
        assertTrue(foundOrderOpt.isPresent());
        Order foundOrder = foundOrderOpt.get();

        assertEquals(order.getId(), foundOrder.getId());
        assertEquals(order.getUser().getId(), foundOrder.getUser().getId());
        assertEquals(order.getSession().getId(), foundOrder.getSession().getId());
        assertEquals(order.getUnits(), foundOrder.getUnits());
        assertEquals(order.getCreditCardNum(), foundOrder.getCreditCardNum());
        assertEquals(order.getDate(), foundOrder.getDate());
        assertEquals(order.isDelivered(), foundOrder.isDelivered());
        assertEquals(initialFreeSeats - 2, foundOrder.getSession().getFreeSeats());
    }

    @Test
    public void testBuyTicketsWithNonExistentUser() {
        Session session = createSession(LocalDateTime.now().plusDays(1));

        assertThrows(InstanceNotFoundException.class,
                () -> orderService.buyTickets(NON_EXISTENT_ID, session.getId(), 2, "1234"));
    }

    @Test
    public void testBuyTicketsWithNonExistentSession() {
        User user = signUpUser("user");

        assertThrows(InstanceNotFoundException.class,
                () -> orderService.buyTickets(user.getId(), NON_EXISTENT_ID, 2, "1234"));
    }

    @Test
    public void testBuyTicketsForAlreadyStartedSession() {
        User user = signUpUser("user");
        Session session = createSession(LocalDateTime.now().minusHours(1));

        assertThrows(SessionAlreadyStartedException.class,
                () -> orderService.buyTickets(user.getId(), session.getId(), 2, "1234"));
    }

    @Test
    public void testBuyTicketsWithoutEnoughSeats() {
        User user = signUpUser("user");
        Session session = createSession(LocalDateTime.now().plusDays(1));
        session.setFreeSeats(1);

        assertThrows(NotEnoughTicketsException.class,
                () -> orderService.buyTickets(user.getId(), session.getId(), 2, "1234"));
    }

    @Test
    public void testFindNoUserOrders() throws InstanceNotFoundException {
        User user = signUpUser("user");
        Block<Order> expectedOrders = new Block<>(new ArrayList<>(), false);

        assertEquals(expectedOrders, orderService.findUserOrders(user.getId(), 0, 1));
    }

    @Test
    public void testFindUserOrders() throws InstanceNotFoundException {
        User user = signUpUser("user");
        Session oldSession = createSession(LocalDateTime.now().plusDays(2));
        Session newSession = createSession(LocalDateTime.now().plusDays(3));

        Order oldOrder = orderDao.save(createOrder(2, oldSession, "1111", LocalDateTime.now().minusDays(2), user));
        Order newOrder = orderDao.save(createOrder(1, newSession, "2222", LocalDateTime.now().minusDays(1), user));

        Block<Order> block = orderService.findUserOrders(user.getId(), 0, 10);

        assertEquals(List.of(newOrder, oldOrder), block.getItems());
        assertFalse(block.getExistMoreItems());
    }

    @Test
    public void testFindUserOrdersPaged() throws InstanceNotFoundException {
        User user = signUpUser("user");
        Session session1 = createSession(LocalDateTime.now().plusDays(2));
        Session session2 = createSession(LocalDateTime.now().plusDays(3));
        Session session3 = createSession(LocalDateTime.now().plusDays(4));

        Order order1 = orderDao.save(createOrder(1, session1, "1111", LocalDateTime.now().minusDays(3), user));
        Order order2 = orderDao.save(createOrder(1, session2, "2222", LocalDateTime.now().minusDays(2), user));
        Order order3 = orderDao.save(createOrder(1, session3, "3333", LocalDateTime.now().minusDays(1), user));

        Block<Order> block = orderService.findUserOrders(user.getId(), 0, 2);

        assertEquals(List.of(order3, order2), block.getItems());
        assertTrue(block.getExistMoreItems());
        assertFalse(block.getItems().contains(order1));
    }

    @Test
    public void testFindUserOrdersWithNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () -> orderService.findUserOrders(NON_EXISTENT_ID, 0, 10));
    }

    @Test
    public void testDeliverTickets() throws InstanceNotFoundException, CreditCardException, SessionAlreadyStartedException, AlreadyDeliveredException {
        Session session = createSession(LocalDateTime.now().plusHours(1));
        User user = signUpUser("user");
        Order order = createOrder(session, "1234", user);
        orderDao.save(order);

        orderService.deliverTickets(order.getId(), order.getCreditCardNum());

        assertTrue(order.isDelivered());

        // Get order from DB
        Optional<Order> deliveredOrderOpt = orderDao.findById(order.getId());
        assertTrue(deliveredOrderOpt.isPresent());
        Order deliveredOrder = deliveredOrderOpt.get();

        assertTrue(deliveredOrder.isDelivered());
    }

    @Test
    public void testDeliverNonExistentTickets() {
        assertThrows(InstanceNotFoundException.class, () -> orderService.deliverTickets(NON_EXISTENT_ID, "1234"));
    }

    @Test
    public void testDeliverAlreadyDeliveredTickets() {
        Session session = createSession(LocalDateTime.now().plusHours(1));

        User user = signUpUser("user");

        Order order = createOrder(session, "1234", user);

        order.setDelivered(true);

        orderDao.save(order);

        assertThrows(AlreadyDeliveredException.class, () -> orderService.deliverTickets(order.getId(), order.getCreditCardNum()));
    }

    @Test
    public void testDeliverTicketsWithAnotherCard() {
        Session session = createSession(LocalDateTime.now().plusHours(1));

        User user = signUpUser("user");

        Order order = createOrder(session, "1234", user);

        orderDao.save(order);

        assertThrows(CreditCardException.class, () -> orderService.deliverTickets(order.getId(), "4321"));
        assertFalse(order.isDelivered());
    }

    @Test
    public void testDeliverTicketsForAlreadyStartedSession() {
        Session session = createSession(LocalDateTime.now().minusDays(1));

        User user = signUpUser("user");

        Order order = createOrder(session, "1234", user);

        orderDao.save(order);

        assertThrows(SessionAlreadyStartedException.class, () -> orderService.deliverTickets(order.getId(), order.getCreditCardNum()));
        assertFalse(order.isDelivered());
    }



}
