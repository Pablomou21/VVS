package es.udc.paproject.backend.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import net.jqwik.api.Arbitraries;
import net.jqwik.time.api.DateTimes;

// Pruebas de integración de OrderDao con la base de datos de test (MySQL).
// Cada @Test se ejecuta una vez con datos aleatorios generados con jqwik (Arbitraries...sample()).
// @DataJpaTest deshace la transacción al acabar cada test: la base de datos queda limpia.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class OrderDaoTest {

    private static final int PAGE_SIZE = 20;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private RoomDao roomDao;

    @Autowired
    private SessionDao sessionDao;

    // Permite vaciar la caché de JPA para que las búsquedas lean de verdad de la base de datos
    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Session session;

    // ---------- Preparación ----------

    // Un pedido necesita un usuario y una sesión guardados (claves foráneas)
    @BeforeEach
    public void setUp() {
        user = createUser("user");

        Movie movie = movieDao.save(new Movie("movie", "summary", (short) 120));
        Room room = roomDao.save(new Room("room", 100));
        session = sessionDao.save(
                new Session(movie, room, LocalDateTime.now().plusDays(1), BigDecimal.TEN));
    }

    // Guarda un usuario (role es NOT NULL en la tabla, el constructor no lo asigna)
    private User createUser(String userName) {
        User newUser = new User(userName, "password", "firstName", "lastName", "user@udc.es");
        newUser.setRole(User.RoleType.USER);
        return userDao.save(newUser);
    }

    // Guarda un pedido del usuario dado con las unidades, la tarjeta y la fecha indicadas
    private Order createOrder(User owner, int units, String creditCardNum, LocalDateTime date) {
        return orderDao.save(new Order(units, session, creditCardNum, date, owner));
    }

    // Guarda un pedido de relleno (1 unidad, tarjeta fija, fecha actual)
    private Order createOrder(User owner) {
        return createOrder(owner, 1, "1234567890123456", LocalDateTime.now());
    }

    // Escribe los cambios pendientes en la base de datos y vacía la caché de JPA
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // ---------- CRUD ----------

    // Al guardar un pedido, la base de datos le asigna un id
    @Test
    public void saveAssignsId() {
        Order order = createOrder(user);

        assertNotNull(order.getId());
    }

    // Un pedido guardado conserva sus unidades al leerlo de la base de datos (unidades aleatorias de 1 a 10)
    @Test
    public void findByIdReturnsSavedUnits() {
        int units = Arbitraries.integers().between(1, 10).sample();

        Order order = createOrder(user, units, "1234567890123456", LocalDateTime.now());
        flushAndClear();
        Order foundOrder = orderDao.findById(order.getId()).get();

        assertEquals(units, foundOrder.getUnits(), "units = " + units);
    }

    // Un pedido guardado conserva su tarjeta al leerlo de la base de datos (tarjeta aleatoria de 16 dígitos)
    @Test
    public void findByIdReturnsSavedCreditCard() {
        String creditCardNum = Arbitraries.strings().numeric().ofLength(16).sample();

        Order order = createOrder(user, 10, creditCardNum, LocalDateTime.now());
        flushAndClear();
        Order foundOrder = orderDao.findById(order.getId()).get();

        assertEquals(creditCardNum, foundOrder.getCreditCardNum(), "creditCardNum = " + creditCardNum);
    }

    // Un pedido borrado por su id deja de existir en la base de datos
    @Test
    public void deleteByIdRemovesOrder() {
        Order order = createOrder(user);
        orderDao.deleteById(order.getId());
        flushAndClear();

        assertFalse(orderDao.existsById(order.getId()));
    }

    // ---------- findByUserIdOrderByDateDesc ----------

    // La búsqueda por usuario devuelve todos sus pedidos (de 1 a 10 pedidos, menos que el tamaño de página)
    @Test
    public void findByUserIdReturnsAllUserOrders() {
        int numOrders = Arbitraries.integers().between(1, 10).sample();

        for (int i = 0; i < numOrders; i++) {
            createOrder(user);
        }
        flushAndClear();
        Slice<Order> orders = orderDao.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, PAGE_SIZE));

        assertEquals(numOrders, orders.getContent().size(), "numOrders = " + numOrders);
    }

    // La búsqueda por usuario no devuelve pedidos de otros usuarios
    @Test
    public void findByUserIdExcludesOtherUsersOrders() {
        User otherUser = createUser("otherUser");

        createOrder(user);
        createOrder(otherUser);
        flushAndClear();
        Slice<Order> orders = orderDao.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, PAGE_SIZE));

        assertTrue(orders.stream().allMatch(o -> o.getUser().getId().equals(user.getId())));
    }

    // La búsqueda por usuario devuelve sus pedidos ordenados de la fecha más reciente a la más antigua
    // (de 2 a 10 pedidos con fechas aleatorias y distintas entre 2020 y 2030)
    @Test
    public void findByUserIdSortsByDateDesc() {
        List<LocalDateTime> dates = DateTimes.dateTimes()
                .atTheEarliest(LocalDateTime.of(2020, 1, 1, 0, 0))
                .atTheLatest(LocalDateTime.of(2030, 12, 31, 23, 59))
                .list().ofMinSize(2).ofMaxSize(10).uniqueElements()
                .sample();

        for (LocalDateTime date : dates) {
            createOrder(user, 1, "1234567890123456", date);
        }
        flushAndClear();
        Slice<Order> orders = orderDao.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, PAGE_SIZE));
        List<LocalDateTime> expectedDates = dates.stream().sorted(Comparator.reverseOrder()).toList();
        List<LocalDateTime> foundDates = orders.stream().map(o -> o.getDate()).toList();

        assertEquals(expectedDates, foundDates, "dates = " + dates);
    }

    // La búsqueda de un usuario sin pedidos devuelve un resultado vacío (frontera: 0 pedidos)
    @Test
    public void findByUserIdWithoutOrdersReturnsEmpty() {
        Slice<Order> orders = orderDao.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, PAGE_SIZE));

        assertTrue(orders.isEmpty());
    }

    // Con exactamente PAGE_SIZE pedidos caben todos en la primera página y no hay siguiente (frontera: 20 pedidos)
    @Test
    public void findByUserIdWithFullPageHasNoNext() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            createOrder(user);
        }
        flushAndClear();
        Slice<Order> orders = orderDao.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, PAGE_SIZE));

        assertFalse(orders.hasNext());
    }

    // Con un pedido más que PAGE_SIZE no caben todos en la primera página y hay siguiente (frontera: 21 pedidos)
    @Test
    public void findByUserIdWithOneMoreThanPageHasNext() {
        for (int i = 0; i < PAGE_SIZE + 1; i++) {
            createOrder(user);
        }
        flushAndClear();
        Slice<Order> orders = orderDao.findByUserIdOrderByDateDesc(user.getId(), PageRequest.of(0, PAGE_SIZE));

        assertTrue(orders.hasNext());
    }

    // ---------- Partición inválida ----------

    // Una tarjeta de 17 caracteres no cabe en la columna (VARCHAR(16)) y se rechaza (frontera: 17 caracteres)
    @Test
    public void creditCardLongerThan16IsRejected() {
        String creditCardNum = Arbitraries.strings().numeric().ofLength(17).sample();

        assertThrows(DataIntegrityViolationException.class,
                () -> createOrder(user, 1, creditCardNum, LocalDateTime.now()));
    }
}
