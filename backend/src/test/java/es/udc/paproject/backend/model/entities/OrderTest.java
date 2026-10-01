package es.udc.paproject.backend.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.time.api.constraints.Precision;

// Pruebas de Order con jqwik: cada @Property se ejecuta hasta 1000 veces con datos aleatorios;
// si el rango tiene menos de 1000 valores, jqwik los prueba todos (generación exhaustiva)
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


    // Un pedido recién creado no está entregado (de 1 a 10 entradas)
    @Property
    public void newOrderIsNotDelivered(
            @ForAll @IntRange(min = 1, max = 10) int units) {

        Session session = createSession(BigDecimal.TEN);
        Order order = new Order(units, session, "1234567890123456", LocalDateTime.now(), null);

        assertFalse(order.isDelivered());
    }

    // Un pedido con 0 o menos entradas es inválido (frontera 0, de -100 a 0)
    @Property
    public void orderWithNonPositiveUnitsIsRejected(
            @ForAll @IntRange(min = -100, max = 0) int units) {

        Session session = createSession(BigDecimal.TEN);

        // Solo el constructor va dentro de la lambda: es lo que debe lanzar la excepción
        assertThrows(IllegalArgumentException.class,
                () -> new Order(units, session, "1234567890123456", LocalDateTime.now(), null));
    }

    // El constructor guarda la fecha sin nanosegundos (cualquier fecha, con nanosegundos)
    @Property
    public void constructorRemovesNanosFromDate(
            @ForAll @Precision(value = ChronoUnit.NANOS) LocalDateTime date) {

        // Sin @Precision jqwik genera fechas sin nanosegundos y el test no comprobaría nada
        Session session = createSession(BigDecimal.TEN);
        Order order = new Order(1, session, "1234567890123456", date, null);

        assertEquals(0, order.getDate().getNano());
    }
}
