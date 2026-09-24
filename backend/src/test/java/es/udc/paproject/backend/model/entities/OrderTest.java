package es.udc.paproject.backend.model.entities;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Pruebas de Order con jqwik: cada @Property se ejecuta 1000 veces con datos aleatorios
public class OrderTest {

    // Crea una sesión con el precio dado (película, sala y fecha son de relleno)
    private Session createSession(BigDecimal price) {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        return new Session(new Movie(), new Room(), tomorrow, price);
    }

    // El precio total es precio x unidades (precio 0-100, de 1 a 10 entradas)
    @Property
    public void totalPriceIsPriceTimesUnits(
            @ForAll @BigRange(min = "0", max = "100") BigDecimal price,
            @ForAll @IntRange(min = 1, max = 10) int units) {

        // Tarjeta, fecha y usuario no afectan al precio
        Session session = createSession(price);
        Order order = new Order(units, session, "1234567890123456", LocalDateTime.now(), null);

        // BigDecimal no admite *, se usa multiply
        BigDecimal expected = price.multiply(BigDecimal.valueOf(units));

        assertEquals(expected, order.getTotalPrice());
    }
}
