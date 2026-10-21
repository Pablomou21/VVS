package es.udc.paproject.backend.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import net.jqwik.api.Example;
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

    // Datos aleatorios: precio entre 0 y 100 y unidades entre 1 y 10
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

    // Valor frontera: el precio cero produce un total cero
    @Example
    public void zeroPriceProducesZeroTotal() {
        Session session = createSession(new BigDecimal("0.00"));
        Order order = new Order(5, session, "1234567890123456", LocalDateTime.now(), null);

        assertEquals(new BigDecimal("0.00"), order.getTotalPrice());
    }

    // Valor frontera: una unidad es la cantidad mínima válida del servicio
    @Example
    public void totalPriceWithOneUnit() {
        Session session = createSession(new BigDecimal("2.50"));
        Order order = new Order(1, session, "1234567890123456", LocalDateTime.now(), null);

        assertEquals(new BigDecimal("2.50"), order.getTotalPrice());
    }

    // Valor frontera: el constructor conserva la cantidad mínima válida
    @Example
    public void constructorStoresOneUnit() {
        Order order = new Order(1, createSession(BigDecimal.TEN),
                "1234567890123456", LocalDateTime.now(), null);

        assertEquals(1, order.getUnits());
    }

    // Valor frontera: diez unidades es el máximo admitido por el DTO de compra
    @Example
    public void totalPriceWithTenUnits() {
        Session session = createSession(new BigDecimal("2.50"));
        Order order = new Order(10, session, "1234567890123456", LocalDateTime.now(), null);

        assertEquals(new BigDecimal("25.00"), order.getTotalPrice());
    }

    // Valor frontera: el constructor conserva la cantidad máxima admitida por el DTO
    @Example
    public void constructorStoresTenUnits() {
        Order order = new Order(10, createSession(BigDecimal.TEN),
                "1234567890123456", LocalDateTime.now(), null);

        assertEquals(10, order.getUnits());
    }


    // Dato aleatorio: unidades entre 1 y 10
    @Property
    public void newOrderIsNotDelivered(
            @ForAll @IntRange(min = 1, max = 10) int units) {

        Session session = createSession(BigDecimal.TEN);
        Order order = new Order(units, session, "1234567890123456", LocalDateTime.now(), null);

        assertFalse(order.isDelivered());
    }

    // Valores frontera: unidades entre -100 y 0, incluido el límite inválido 0
    @Property
    public void orderWithNonPositiveUnitsIsRejected(
            @ForAll @IntRange(min = -100, max = 0) int units) {

        Session session = createSession(BigDecimal.TEN);

        // Solo el constructor va dentro de la lambda: es lo que debe lanzar la excepción
        assertThrows(IllegalArgumentException.class,
                () -> new Order(units, session, "1234567890123456", LocalDateTime.now(), null));
    }

    // Dato aleatorio: fechas con precisión de nanosegundos
    @Property
    public void constructorRemovesNanosFromDate(
            @ForAll @Precision(value = ChronoUnit.NANOS) LocalDateTime date) {

        // Sin @Precision jqwik genera fechas sin nanosegundos y el test no comprobaría nada
        Session session = createSession(BigDecimal.TEN);
        Order order = new Order(1, session, "1234567890123456", date, null);

        assertEquals(0, order.getDate().getNano());
    }
}
