package es.udc.paproject.backend.test.model.entities;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.Session;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderTest {

    private Session createSession(BigDecimal price) {
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        return new Session(new Movie(), new Room(), date, price);
    }

    @Test
    public void testGetTotalPrice() {
        BigDecimal sessionPrice = BigDecimal.valueOf(7.50);
        Session session = createSession(sessionPrice);

        int units = 5;
        Order order = new Order(units, session, "1234567890123456", LocalDateTime.now(), null);

        BigDecimal expectedTotalPrice = sessionPrice.multiply(BigDecimal.valueOf(units));

        assertEquals(expectedTotalPrice, order.getTotalPrice());

    }
}
