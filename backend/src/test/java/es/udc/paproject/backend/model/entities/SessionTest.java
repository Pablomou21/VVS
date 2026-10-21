package es.udc.paproject.backend.model.entities;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// pruebas de Session con jqwik: cada @Property se ejecuta 1000 veces con datos aleatorios
public class SessionTest {

    // Dato aleatorio: la sesión conserva la película recibida
    @Property
    public void constructorStoresMovie(
            @ForAll @StringLength(min = 1, max = 50) String movieTitle) {
        Movie movie = new Movie(movieTitle, "summary", (short) 120);
        Session session = new Session(movie, new Room("room", 100),
                LocalDateTime.now().plusDays(1), BigDecimal.TEN);
        assertSame(movie, session.getMovie());
    }

    // Dato aleatorio: la sesión conserva la sala recibida
    @Property
    public void constructorStoresRoom(@ForAll @IntRange(min = 1, max = 500) int capacity) {
        Room room = new Room("room", capacity);
        Session session = new Session(new Movie(), room, LocalDateTime.now().plusDays(1), BigDecimal.TEN);
        assertSame(room, session.getRoom());
    }

    // Dato aleatorio: la sesión conserva el precio recibido
    @Property
    public void constructorStoresPrice(@ForAll @BigRange(min = "0", max = "100") BigDecimal price) {
        Session session = new Session(new Movie(), new Room("room", 100),
                LocalDateTime.now().plusDays(1), price);
        assertEquals(price, session.getPrice());
    }

    // Valor frontera: una sesión puede tener precio cero
    @Example
    public void constructorStoresZeroPrice() {
        Session session = new Session(new Movie(), new Room("room", 100),
                LocalDateTime.now().plusDays(1), BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, session.getPrice());
    }

    // Dato aleatorio: las plazas libres empiezan siendo la capacidad de la sala
    @Property
    public void constructorInitializesFreeSeatsWithRoomCapacity(
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

        Room room = new Room("room", capacity);
        Session session = new Session(new Movie(), room, LocalDateTime.now(), BigDecimal.TEN);

        assertEquals(capacity, session.getFreeSeats());
    }

    // Valor frontera: una sala sin plazas libres deja cero plazas en la sesión
    @Example
    public void constructorInitializesZeroFreeSeatsForZeroCapacity() {
        Session session = new Session(new Movie(), new Room("room", 0),
                LocalDateTime.now().plusDays(1), BigDecimal.TEN);

        assertEquals(0, session.getFreeSeats());
    }

    // Datos aleatorios: fecha y nanosegundos distintos de cero
    @Property
    public void constructorRemovesNanosecondsFromDate(
            @ForAll @IntRange(min = 1, max = 365) int days,
            @ForAll @IntRange(min = 1, max = 999999999) int nanoseconds) {

        LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 30, 15, nanoseconds).plusDays(days);
        Session session = new Session(new Movie(), new Room("room", 100), date, BigDecimal.TEN);

        assertEquals(date.withNano(0), session.getDate());
    }

    // Dato aleatorio: los setters actualizan la película
    @Property
    public void setterUpdatesMovie(@ForAll @StringLength(min = 1, max = 50) String movieTitle) {
        Movie movie = new Movie(movieTitle, "updated summary", (short) 90);
        Session session = new Session();
        session.setMovie(movie);
        assertSame(movie, session.getMovie());
    }

    // Dato aleatorio: los setters actualizan la sala
    @Property
    public void setterUpdatesRoom(@ForAll @IntRange(min = 1, max = 500) int capacity) {
        Room room = new Room("updated room", capacity);
        Session session = new Session();
        session.setRoom(room);
        assertSame(room, session.getRoom());
    }

    // Dato aleatorio: los setters actualizan la fecha
    @Property
    public void setterUpdatesDate(@ForAll @IntRange(min = 1, max = 365) int days) {
        LocalDateTime date = LocalDateTime.of(2026, 6, 1, 20, 0).plusDays(days);
        Session session = new Session();
        session.setDate(date);
        assertEquals(date, session.getDate());
    }

    // Dato aleatorio: los setters actualizan el precio
    @Property
    public void setterUpdatesPrice(@ForAll @BigRange(min = "0", max = "100") BigDecimal price) {
        Session session = new Session();
        session.setPrice(price);
        assertEquals(price, session.getPrice());
    }

    // Dato aleatorio: los setters actualizan las plazas libres
    @Property
    public void setterUpdatesFreeSeats(@ForAll @IntRange(min = 1, max = 500) int freeSeats) {
        Session session = new Session();
        session.setFreeSeats(freeSeats);
        assertEquals(freeSeats, session.getFreeSeats());
    }

    // Valor frontera: el setter admite cero plazas libres
    @Example
    public void setterUpdatesFreeSeatsToZero() {
        Session session = new Session();
        session.setFreeSeats(0);

        assertEquals(0, session.getFreeSeats());
    }

    // Dato aleatorio: el identificador se puede leer después de asignarlo
    @Property
    public void setterUpdatesId(@ForAll @LongRange(min = 1, max = 1000000) long id) {
        Session session = new Session();
        session.setId(id);
        assertEquals(id, session.getId());
    }

    // Dato aleatorio: la versión se puede leer después de asignarla
    @Property
    public void setterUpdatesVersion(@ForAll @LongRange(min = 0, max = 1000000) long version) {
        Session session = new Session();
        session.setVersion(version);
        assertEquals(version, session.getVersion());
    }
}
