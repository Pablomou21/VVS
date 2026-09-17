package es.udc.paproject.backend.model.services;

import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.exceptions.*;

public interface OrderService {
    // Buy tickets
    Order buyTickets(Long userId, Long sessionId, int numTickets, String creditCardNum)
            throws InstanceNotFoundException, SessionAlreadyStartedException, NotEnoughTicketsException;  //excepciones bien?

    // View order history
    Block<Order> findUserOrders(Long userId, int page, int size)
            throws InstanceNotFoundException;

    // Deliver tickets for an order by a ticket seller
    void deliverTickets(Long orderId, String creditCardNum)
            throws InstanceNotFoundException,
            CreditCardException, SessionAlreadyStartedException,
            AlreadyDeliveredException;
}
