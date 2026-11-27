package es.udc.paproject.backend.model.services;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.entities.OrderDao;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.entities.UserDao;
import es.udc.paproject.backend.model.exceptions.AlreadyDeliveredException;
import es.udc.paproject.backend.model.exceptions.CreditCardException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.NotEnoughTicketsException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import net.jqwik.api.Arbitraries;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    private static final String CARD = "1234567812345678";

    @Mock
    private OrderDao orderDao;

    @Mock
    private UserDao userDao;

    @Mock
    private SessionDao sessionDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Long randomId() {
        return Arbitraries.longs().between(1, Long.MAX_VALUE).sample();
    }

    private int randomInt(int min, int max) {
        return Arbitraries.integers().between(min, max).sample();
    }

    private Session session(LocalDateTime date, int freeSeats) {
        Session session = new Session(new Movie("Titulo", "Resumen", (short) 90), new Room(), date, BigDecimal.TEN);
        session.setFreeSeats(freeSeats);
        return session;
    }

    private Session futureSession(int freeSeats) {
        return session(LocalDateTime.now().plusDays(1), freeSeats);
    }

    private Session pastSession(int freeSeats) {
        return session(LocalDateTime.now().minusHours(1), freeSeats);
    }

    // El usuario 1 existe y la sesión 2 es la dada
    private User userAndSessionExist(Session session) {
        User user = new User();
        when(userDao.findById(1L)).thenReturn(Optional.of(user));
        when(sessionDao.findById(2L)).thenReturn(Optional.of(session));
        return user;
    }

    // Compra válida de numTickets y devuelve el pedido que el servicio pasa a save
    private Order buyAndCaptureOrder(int numTickets) throws Exception {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orderDao.save(captor.capture())).thenReturn(new Order());
        orderService.buyTickets(1L, 2L, numTickets, CARD);
        return captor.getValue();
    }

    private Order order(LocalDateTime sessionDate, boolean delivered) {
        Order order = new Order(2, session(sessionDate, 10), CARD, LocalDateTime.now(), new User());
        order.setDelivered(delivered);
        return order;
    }

    // ========== buyTickets ==========

    // ---------- Partición válida: hay plazas suficientes ----------

    // Dato aleatorio: de 1 a 10 entradas en una sesión con 10 plazas
    @Test
    public void buyTicketsReturnsSavedOrder() throws Exception {
        userAndSessionExist(futureSession(10));
        Order saved = new Order();
        when(orderDao.save(any())).thenReturn(saved);

        Order result = orderService.buyTickets(1L, 2L, randomInt(1, 10), CARD);

        assertSame(saved, result);
    }

    // Datos aleatorios: de 1 a 100 plazas libres y de 1 a ese número de entradas
    @Test
    public void buyTicketsDecreasesFreeSeats() throws Exception {
        int freeSeats = randomInt(1, 100);
        int numTickets = randomInt(1, freeSeats);
        Session session = futureSession(freeSeats);
        userAndSessionExist(session);

        orderService.buyTickets(1L, 2L, numTickets, CARD);

        assertEquals(freeSeats - numTickets, session.getFreeSeats());
    }

    // Valor frontera: se compran exactamente todas las plazas libres
    @Test
    public void buyTicketsWithAllFreeSeatsLeavesZero() throws Exception {
        int freeSeats = randomInt(1, 100);
        Session session = futureSession(freeSeats);
        userAndSessionExist(session);

        orderService.buyTickets(1L, 2L, freeSeats, CARD);

        assertEquals(0, session.getFreeSeats());
    }

    // Valor frontera: 1 entrada, el menor número válido
    @Test
    public void buyTicketsWithOneTicketDecreasesOneSeat() throws Exception {
        Session session = futureSession(10);
        userAndSessionExist(session);

        orderService.buyTickets(1L, 2L, 1, CARD);

        assertEquals(9, session.getFreeSeats());
    }

    // Dato aleatorio: de 1 a 10 entradas, el pedido guardado tiene esas unidades
    @Test
    public void buyTicketsSavesOrderWithUnits() throws Exception {
        int numTickets = randomInt(1, 10);
        userAndSessionExist(futureSession(10));

        Order order = buyAndCaptureOrder(numTickets);

        assertEquals(numTickets, order.getUnits());
    }

    // Partición: el pedido guardado es de la sesión comprada
    @Test
    public void buyTicketsSavesOrderWithSession() throws Exception {
        Session session = futureSession(10);
        userAndSessionExist(session);

        Order order = buyAndCaptureOrder(2);

        assertSame(session, order.getSession());
    }

    // Partición: el pedido guardado es del usuario que compra
    @Test
    public void buyTicketsSavesOrderWithUser() throws Exception {
        User user = userAndSessionExist(futureSession(10));

        Order order = buyAndCaptureOrder(2);

        assertSame(user, order.getUser());
    }

    // Partición: el pedido guardado tiene la tarjeta usada
    @Test
    public void buyTicketsSavesOrderWithCreditCard() throws Exception {
        userAndSessionExist(futureSession(10));

        Order order = buyAndCaptureOrder(2);

        assertEquals(CARD, order.getCreditCardNum());
    }

    // Partición: un pedido nuevo no está entregado
    @Test
    public void buyTicketsSavesUndeliveredOrder() throws Exception {
        userAndSessionExist(futureSession(10));

        Order order = buyAndCaptureOrder(2);

        assertFalse(order.isDelivered());
    }

    // Partición: la fecha del pedido es el instante de la compra (sin nanosegundos)
    @Test
    public void buyTicketsSavesOrderWithCurrentDate() throws Exception {
        userAndSessionExist(futureSession(10));
        LocalDateTime before = LocalDateTime.now().withNano(0);

        Order order = buyAndCaptureOrder(2);

        LocalDateTime after = LocalDateTime.now();
        assertTrue(!order.getDate().isBefore(before) && !order.getDate().isAfter(after));
    }

    // ---------- Partición inválida: usuario o sesión inexistentes ----------

    // Dato aleatorio: id de usuario inexistente
    @Test
    public void buyTicketsWithNonExistentUserThrows() {
        Long userId = randomId();
        when(userDao.findById(userId)).thenReturn(Optional.empty());

        assertThrows(InstanceNotFoundException.class, () -> orderService.buyTickets(userId, 2L, 2, CARD));
    }

    // Dato aleatorio: id de usuario inexistente, la excepción indica que falta el usuario
    @Test
    public void buyTicketsUserNotFoundExceptionNameIsUserEntity() {
        Long userId = randomId();
        when(userDao.findById(userId)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> orderService.buyTickets(userId, 2L, 2, CARD),
                InstanceNotFoundException.class);

        assertEquals("project.entities.user", e.getName());
    }

    // Dato aleatorio: id de usuario inexistente, no se llega a buscar la sesión
    @Test
    public void buyTicketsWithNonExistentUserDoesNotSearchSession() {
        Long userId = randomId();
        when(userDao.findById(userId)).thenReturn(Optional.empty());

        catchThrowable(() -> orderService.buyTickets(userId, 2L, 2, CARD));

        verifyNoInteractions(sessionDao);
    }

    // Dato aleatorio: id de sesión inexistente
    @Test
    public void buyTicketsWithNonExistentSessionThrows() {
        Long sessionId = randomId();
        when(userDao.findById(1L)).thenReturn(Optional.of(new User()));
        when(sessionDao.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(InstanceNotFoundException.class, () -> orderService.buyTickets(1L, sessionId, 2, CARD));
    }

    // Dato aleatorio: id de sesión inexistente, la excepción indica que falta la sesión
    @Test
    public void buyTicketsSessionNotFoundExceptionNameIsSessionEntity() {
        Long sessionId = randomId();
        when(userDao.findById(1L)).thenReturn(Optional.of(new User()));
        when(sessionDao.findById(sessionId)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> orderService.buyTickets(1L, sessionId, 2, CARD),
                InstanceNotFoundException.class);

        assertEquals("project.entities.session", e.getName());
    }

    // ---------- Partición inválida: sesión empezada ----------

    // Partición: sesión que empezó hace 1 hora
    @Test
    public void buyTicketsForStartedSessionThrows() {
        userAndSessionExist(pastSession(10));

        assertThrows(SessionAlreadyStartedException.class, () -> orderService.buyTickets(1L, 2L, 2, CARD));
    }

    // Orden de las comprobaciones: sesión empezada y sin plazas, gana sesión empezada
    @Test
    public void buyTicketsForStartedSessionWithoutSeatsThrowsSessionStarted() {
        userAndSessionExist(pastSession(0));

        assertThrows(SessionAlreadyStartedException.class, () -> orderService.buyTickets(1L, 2L, 2, CARD));
    }

    // ---------- Partición inválida: no hay plazas suficientes ----------

    // Valor frontera: una entrada más que las plazas libres
    @Test
    public void buyTicketsWithMoreTicketsThanFreeSeatsThrows() {
        int freeSeats = randomInt(0, 100);
        userAndSessionExist(futureSession(freeSeats));

        assertThrows(NotEnoughTicketsException.class,
                () -> orderService.buyTickets(1L, 2L, freeSeats + 1, CARD));
    }

    // Dato aleatorio: de 0 a 100 plazas, la excepción informa de las que quedan
    @Test
    public void notEnoughTicketsExceptionKeepsFreeSeats() {
        int freeSeats = randomInt(0, 100);
        userAndSessionExist(futureSession(freeSeats));

        NotEnoughTicketsException e = catchThrowableOfType(
                () -> orderService.buyTickets(1L, 2L, freeSeats + 1, CARD), NotEnoughTicketsException.class);

        assertEquals(freeSeats, e.getNumberTicketsLeft());
    }

    // Dato aleatorio: sin plazas suficientes, las plazas libres no cambian
    @Test
    public void buyTicketsWithoutEnoughSeatsKeepsFreeSeats() {
        int freeSeats = randomInt(0, 100);
        Session session = futureSession(freeSeats);
        userAndSessionExist(session);

        catchThrowable(() -> orderService.buyTickets(1L, 2L, freeSeats + 1, CARD));

        assertEquals(freeSeats, session.getFreeSeats());
    }

    // Dato aleatorio: sin plazas suficientes, no se guarda ningún pedido
    @Test
    public void buyTicketsWithoutEnoughSeatsDoesNotSave() {
        int freeSeats = randomInt(0, 100);
        userAndSessionExist(futureSession(freeSeats));

        catchThrowable(() -> orderService.buyTickets(1L, 2L, freeSeats + 1, CARD));

        verify(orderDao, never()).save(any());
    }

    // ---------- Partición inválida: número de entradas no positivo (falla, issue #1) ----------

    // Valor frontera: 0 entradas
    @Test
    public void buyTicketsWithZeroTicketsIsRejected() {
        userAndSessionExist(futureSession(10));

        assertThrows(IllegalArgumentException.class, () -> orderService.buyTickets(1L, 2L, 0, CARD));
    }

    // Dato aleatorio: de -100 a -1 entradas
    @Test
    public void buyTicketsWithNegativeTicketsIsRejected() {
        int numTickets = randomInt(-100, -1);
        userAndSessionExist(futureSession(10));

        assertThrows(IllegalArgumentException.class, () -> orderService.buyTickets(1L, 2L, numTickets, CARD));
    }

    // ========== findUserOrders ==========

    // ---------- Partición válida: usuario existente ----------

    // Dato aleatorio: de 0 a 5 pedidos, el bloque contiene los de la página
    @Test
    public void findUserOrdersReturnsSliceContent() throws InstanceNotFoundException {
        Long userId = randomId();
        List<Order> orders = Arbitraries.just(new Order()).list().ofMaxSize(5).sample();
        when(userDao.existsById(userId)).thenReturn(true);
        when(orderDao.findByUserIdOrderByDateDesc(eq(userId), any()))
                .thenReturn(new SliceImpl<>(orders, PageRequest.of(0, 10), false));

        Block<Order> result = orderService.findUserOrders(userId, 0, 10);

        assertEquals(orders, result.getItems());
    }

    // Partición: hay más páginas
    @Test
    public void findUserOrdersWithNextPageHasMoreItems() throws InstanceNotFoundException {
        Long userId = randomId();
        when(userDao.existsById(userId)).thenReturn(true);
        when(orderDao.findByUserIdOrderByDateDesc(eq(userId), any()))
                .thenReturn(new SliceImpl<>(List.of(new Order()), PageRequest.of(0, 1), true));

        Block<Order> result = orderService.findUserOrders(userId, 0, 1);

        assertTrue(result.getExistMoreItems());
    }

    // Partición: es la última página
    @Test
    public void findUserOrdersWithoutNextPageHasNoMoreItems() throws InstanceNotFoundException {
        Long userId = randomId();
        when(userDao.existsById(userId)).thenReturn(true);
        when(orderDao.findByUserIdOrderByDateDesc(eq(userId), any()))
                .thenReturn(new SliceImpl<>(List.of(new Order()), PageRequest.of(0, 1), false));

        Block<Order> result = orderService.findUserOrders(userId, 0, 1);

        assertFalse(result.getExistMoreItems());
    }

    // Datos aleatorios: página de 0 a 100 y tamaño de 1 a 50, se pasan al DAO
    @Test
    public void findUserOrdersPassesPageAndSizeToDao() throws InstanceNotFoundException {
        Long userId = randomId();
        int page = randomInt(0, 100);
        int size = randomInt(1, 50);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(userDao.existsById(userId)).thenReturn(true);
        when(orderDao.findByUserIdOrderByDateDesc(eq(userId), captor.capture()))
                .thenReturn(new SliceImpl<>(List.of()));

        orderService.findUserOrders(userId, page, size);

        assertEquals(PageRequest.of(page, size), captor.getValue());
    }

    // Valores frontera: primera página (0) y tamaño mínimo (1)
    @Test
    public void findUserOrdersWithFirstPageAndSizeOnePassesThemToDao() throws InstanceNotFoundException {
        Long userId = randomId();
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(userDao.existsById(userId)).thenReturn(true);
        when(orderDao.findByUserIdOrderByDateDesc(eq(userId), captor.capture()))
                .thenReturn(new SliceImpl<>(List.of()));

        orderService.findUserOrders(userId, 0, 1);

        assertEquals(PageRequest.of(0, 1), captor.getValue());
    }

    // ---------- Partición inválida: página o tamaño fuera de rango ----------

    // Valor frontera: página -1, justo antes de la primera página
    @Test
    public void findUserOrdersWithNegativePageThrows() {
        Long userId = randomId();
        when(userDao.existsById(userId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> orderService.findUserOrders(userId, -1, 10));
    }

    // Valor frontera: tamaño 0, justo por debajo del tamaño mínimo
    @Test
    public void findUserOrdersWithZeroSizeThrows() {
        Long userId = randomId();
        when(userDao.existsById(userId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> orderService.findUserOrders(userId, 0, 0));
    }

    // ---------- Partición inválida: usuario inexistente ----------

    // Dato aleatorio: id de usuario inexistente
    @Test
    public void findUserOrdersWithNonExistentUserThrows() {
        Long userId = randomId();
        when(userDao.existsById(userId)).thenReturn(false);

        assertThrows(InstanceNotFoundException.class, () -> orderService.findUserOrders(userId, 0, 10));
    }

    // Dato aleatorio: id de usuario inexistente, no se consultan sus pedidos
    @Test
    public void findUserOrdersWithNonExistentUserDoesNotQueryOrders() {
        Long userId = randomId();
        when(userDao.existsById(userId)).thenReturn(false);

        catchThrowable(() -> orderService.findUserOrders(userId, 0, 10));

        verifyNoInteractions(orderDao);
    }

    // ========== deliverTickets ==========

    // ---------- Partición válida: sesión futura, tarjeta correcta, no entregado ----------

    // Partición: entrega correcta
    @Test
    public void deliverTicketsMarksOrderAsDelivered() throws Exception {
        Long orderId = randomId();
        Order order = order(LocalDateTime.now().plusDays(1), false);
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order));

        orderService.deliverTickets(orderId, CARD);

        assertTrue(order.isDelivered());
    }

    // ---------- Partición inválida: pedido inexistente ----------

    // Dato aleatorio: id de pedido inexistente
    @Test
    public void deliverTicketsWithNonExistentOrderThrows() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(InstanceNotFoundException.class, () -> orderService.deliverTickets(orderId, CARD));
    }

    // Dato aleatorio: id de pedido inexistente, la excepción indica que falta el pedido
    @Test
    public void orderNotFoundExceptionNameIsOrderEntity() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> orderService.deliverTickets(orderId, CARD),
                InstanceNotFoundException.class);

        assertEquals("project.entities.order", e.getName());
    }

    // ---------- Partición inválida: sesión empezada ----------

    // Partición: sesión que empezó hace 1 hora
    @Test
    public void deliverTicketsForStartedSessionThrows() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().minusHours(1), false)));

        assertThrows(SessionAlreadyStartedException.class, () -> orderService.deliverTickets(orderId, CARD));
    }

    // ---------- Partición inválida: tarjeta que no coincide ----------

    // Dato aleatorio: tarjeta de 16 dígitos distinta de la usada en la compra
    @Test
    public void deliverTicketsWithWrongCreditCardThrows() {
        Long orderId = randomId();
        String card = Arbitraries.strings().numeric().ofLength(16).filter(c -> !c.equals(CARD)).sample();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().plusDays(1), false)));

        assertThrows(CreditCardException.class, () -> orderService.deliverTickets(orderId, card));
    }

    // Valor frontera: tarjeta que solo cambia el último dígito
    @Test
    public void deliverTicketsWithCardDifferingInLastDigitThrows() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().plusDays(1), false)));

        assertThrows(CreditCardException.class, () -> orderService.deliverTickets(orderId, "1234567812345679"));
    }

    // Valor especial: tarjeta null
    @Test
    public void deliverTicketsWithNullCreditCardThrows() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().plusDays(1), false)));

        assertThrows(CreditCardException.class, () -> orderService.deliverTickets(orderId, null));
    }

    // Dato aleatorio: la excepción guarda el id del pedido
    @Test
    public void creditCardExceptionKeepsOrderId() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().plusDays(1), false)));

        CreditCardException e = catchThrowableOfType(() -> orderService.deliverTickets(orderId, "0000000000000000"),
                CreditCardException.class);

        assertEquals(orderId, e.getOrderId());
    }

    // Partición: con tarjeta incorrecta el pedido no se entrega
    @Test
    public void deliverTicketsWithWrongCreditCardDoesNotDeliver() {
        Long orderId = randomId();
        Order order = order(LocalDateTime.now().plusDays(1), false);
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order));

        catchThrowable(() -> orderService.deliverTickets(orderId, "0000000000000000"));

        assertFalse(order.isDelivered());
    }

    // ---------- Partición inválida: pedido ya entregado ----------

    // Partición: pedido entregado antes
    @Test
    public void deliverTicketsAlreadyDeliveredThrows() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().plusDays(1), true)));

        assertThrows(AlreadyDeliveredException.class, () -> orderService.deliverTickets(orderId, CARD));
    }

    // ---------- Orden de las comprobaciones ----------

    // Sesión empezada, tarjeta incorrecta y ya entregado: gana sesión empezada
    @Test
    public void deliverTicketsChecksStartedSessionFirst() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().minusHours(1), true)));

        assertThrows(SessionAlreadyStartedException.class,
                () -> orderService.deliverTickets(orderId, "0000000000000000"));
    }

    // Tarjeta incorrecta y ya entregado: gana tarjeta incorrecta
    @Test
    public void deliverTicketsChecksCreditCardBeforeDelivered() {
        Long orderId = randomId();
        when(orderDao.findById(orderId)).thenReturn(Optional.of(order(LocalDateTime.now().plusDays(1), true)));

        assertThrows(CreditCardException.class, () -> orderService.deliverTickets(orderId, "0000000000000000"));
    }

}
