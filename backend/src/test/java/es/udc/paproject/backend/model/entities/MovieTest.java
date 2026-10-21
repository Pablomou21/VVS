package es.udc.paproject.backend.model.entities;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MovieTest {

    // Partición equivalente: título no vacío dentro del tamaño admitido por el DTO
    @Property
    public void constructorStoresTitle(@ForAll @StringLength(min = 1, max = 60) String title) {
        Movie movie = new Movie(title, "summary", (short) 120);

        assertEquals(title, movie.getTitle());
    }

    // Partición equivalente: resumen no vacío
    @Property
    public void constructorStoresSummary(@ForAll @StringLength(min = 1, max = 2000) String summary) {
        Movie movie = new Movie("title", summary, (short) 120);

        assertEquals(summary, movie.getSummary());
    }

    // Partición equivalente: duración positiva representable como short
    @Property
    public void constructorStoresDuration(@ForAll @IntRange(min = 1, max = 32767) int duration) {
        Movie movie = new Movie("title", "summary", (short) duration);

        assertEquals((short) duration, movie.getDuration());
    }

    // Partición equivalente: el setter sustituye el título
    @Property
    public void setterUpdatesTitle(@ForAll @StringLength(min = 1, max = 60) String title) {
        Movie movie = new Movie("previous title", "summary", (short) 120);
        movie.setTitle(title);

        assertEquals(title, movie.getTitle());
    }

    // Partición equivalente: el setter sustituye el resumen
    @Property
    public void setterUpdatesSummary(@ForAll @StringLength(min = 1, max = 2000) String summary) {
        Movie movie = new Movie("title", "previous summary", (short) 120);
        movie.setSummary(summary);

        assertEquals(summary, movie.getSummary());
    }

    // Partición equivalente: el setter sustituye la duración
    @Property
    public void setterUpdatesDuration(@ForAll @IntRange(min = 1, max = 32767) int duration) {
        Movie movie = new Movie("title", "summary", (short) 120);
        movie.setDuration((short) duration);

        assertEquals((short) duration, movie.getDuration());
    }

    // Dato aleatorio: se puede asignar y recuperar el identificador
    @Property
    public void setterUpdatesId(@ForAll @LongRange(min = 1, max = 1000000) long id) {
        Movie movie = new Movie();
        movie.setId(id);

        assertEquals(id, movie.getId());
    }
}
