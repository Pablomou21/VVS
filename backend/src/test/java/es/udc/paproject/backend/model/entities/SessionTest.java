package es.udc.paproject.backend.model.entities;

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

    // la sesión conserva la película, la sala y el precio recibidos
    @Property
    public void constructorStoresSessionData(
            @ForAll @StringLength(min = 1, max = 50) String movieTitle,
            @ForAll @IntRange(min = 1, max = 500) int capacity,
            @ForAll @BigRange(min = "0", max = "100") BigDecimal price,
            @ForAll @IntRange(min = 1, max = 365) int days) {

        Movie movie = new Movie(movieTitle, "summary", (short) 120);
        Room room = new Room("room", capacity);
        LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 30);
        Session session = new Session(movie, room, date.plusDays(days), price);

        assertSame(movie, session.getMovie());
        assertSame(room, session.getRoom());
        assertEquals(price, session.getPrice());
    }

    // las plazas libres empiezan siendo la capacidad completa de la sala
    @Property
    public void constructorInitializesFreeSeatsWithRoomCapacity(
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

        Room room = new Room("room", capacity);
        Session session = new Session(new Movie(), room, LocalDateTime.now(), BigDecimal.TEN);

        assertEquals(capacity, session.getFreeSeats());
    }

    // la fecha de la sesión se guarda sin nanosegundos
    @Property
    public void constructorRemovesNanosecondsFromDate(
            @ForAll @IntRange(min = 1, max = 365) int days,
            @ForAll @IntRange(min = 1, max = 999999999) int nanoseconds) {

        LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 30, 15, nanoseconds).plusDays(days);
        Session session = new Session(new Movie(), new Room("room", 100), date, BigDecimal.TEN);

        assertEquals(date.withNano(0), session.getDate());
    }

    // los setters permiten actualizar los datos mutables de la sesión
    @Property
    public void settersUpdateSessionData(
            @ForAll @IntRange(min = 1, max = 500) int initialCapacity,
            @ForAll @IntRange(min = 1, max = 500) int updatedFreeSeats,
            @ForAll @BigRange(min = "0", max = "100") BigDecimal updatedPrice,
            @ForAll @IntRange(min = 1, max = 365) int days) {

        Session session = new Session(new Movie(), new Room("room", initialCapacity),
                LocalDateTime.now(), BigDecimal.ONE);
                
        Movie movie = new Movie("updated movie", "updated summary", (short) 90);
        Room room = new Room("updated room", 200);
        LocalDateTime date = LocalDateTime.of(2026, 6, 1, 20, 0).plusDays(days);

        session.setMovie(movie);
        session.setRoom(room);
        session.setDate(date);
        session.setPrice(updatedPrice);
        session.setFreeSeats(updatedFreeSeats);

        assertSame(movie, session.getMovie());
        assertSame(room, session.getRoom());
        assertEquals(date, session.getDate());
        assertEquals(updatedPrice, session.getPrice());
        assertEquals(updatedFreeSeats, session.getFreeSeats());
    }

    // los identificadores y la versión se pueden leer después de asignarlos
    @Property
    public void settersUpdateIdAndVersion(
            @ForAll @LongRange(min = 1, max = 1000000) long id,
            @ForAll @LongRange(min = 0, max = 1000000) long version) {

        Session session = new Session();
        session.setId(id);
        session.setVersion(version);

        assertEquals(id, session.getId());
        assertEquals(version, session.getVersion());
    }
}
