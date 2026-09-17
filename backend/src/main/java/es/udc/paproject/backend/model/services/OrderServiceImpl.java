package es.udc.paproject.backend.model.services;

import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.entities.OrderDao;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.entities.UserDao;
import es.udc.paproject.backend.model.exceptions.AlreadyDeliveredException;
import es.udc.paproject.backend.model.exceptions.CreditCardException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.NotEnoughTicketsException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private SessionDao sessionDao;

    @Override
    public Order buyTickets(Long userId, Long sessionId, int numTickets, String creditCardNum)
            throws InstanceNotFoundException, SessionAlreadyStartedException, NotEnoughTicketsException {

        Optional<User> optionalUser = userDao.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new InstanceNotFoundException("project.entities.user", userId);
        }

        Optional<Session> optionalSession = sessionDao.findById(sessionId);
        if (optionalSession.isEmpty()) {
            throw new InstanceNotFoundException("project.entities.session", sessionId);
        }

        Session session = optionalSession.get();

        if (session.getDate().isBefore(LocalDateTime.now())) {
            throw new SessionAlreadyStartedException(sessionId);
        }

        if (session.getFreeSeats() < numTickets) {
            throw new NotEnoughTicketsException(session.getFreeSeats());
        }

        session.setFreeSeats(session.getFreeSeats() - numTickets);

        Order order = new Order(numTickets, session, creditCardNum, LocalDateTime.now(), optionalUser.get());

        return orderDao.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Block<Order> findUserOrders(Long userId, int page, int size) throws InstanceNotFoundException {

        if (!userDao.existsById(userId)) {
            throw new InstanceNotFoundException("project.entities.user", userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Slice<Order> slice = orderDao.findByUserIdOrderByDateDesc(userId, pageable);

        return new Block<>(slice.getContent(), slice.hasNext());
    }

    @Override
    public void deliverTickets(Long orderId, String creditCardNum)
            throws InstanceNotFoundException, CreditCardException, SessionAlreadyStartedException, AlreadyDeliveredException {

        Optional<Order> order = orderDao.findById(orderId);

        if (order.isEmpty()) {
            throw new InstanceNotFoundException("project.entities.order", orderId);
        }

        Order obtainedOrder = order.get();

        if (obtainedOrder.getSession().getDate().isBefore(LocalDateTime.now())) {
            throw new SessionAlreadyStartedException(obtainedOrder.getSession().getId());
        }

        if (!obtainedOrder.getCreditCardNum().equals(creditCardNum)) {
            throw new CreditCardException(orderId, creditCardNum);
        }

        if (obtainedOrder.isDelivered()) {
            throw new AlreadyDeliveredException();
        }

        obtainedOrder.setDelivered(true);
    }
}