package es.udc.paproject.backend.model.services;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Session;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class BillboardEntryTest {

    // Partición equivalente: el constructor conserva la película recibida
    @Test
    public void constructorStoresMovie() {
        Movie movie = new Movie("Title", "Summary", (short) 120);
        BillboardEntry entry = new BillboardEntry(movie, List.of());

        assertSame(movie, entry.getMovie());
    }

    // Partición equivalente: el constructor conserva la lista de sesiones recibida
    @Test
    public void constructorStoresSessions() {
        List<Session> sessions = new ArrayList<>();
        BillboardEntry entry = new BillboardEntry(new Movie(), sessions);

        assertSame(sessions, entry.getSessions());
    }

    // Partición equivalente: setMovie sustituye la película
    @Test
    public void setterUpdatesMovie() {
        Movie movie = new Movie("Updated title", "Summary", (short) 90);
        BillboardEntry entry = new BillboardEntry(new Movie(), List.of());
        entry.setMovie(movie);

        assertSame(movie, entry.getMovie());
    }

    // Partición equivalente: setSessions sustituye la lista de sesiones
    @Test
    public void setterUpdatesSessions() {
        List<Session> sessions = new ArrayList<>();
        BillboardEntry entry = new BillboardEntry(new Movie(), List.of());
        entry.setSessions(sessions);

        assertSame(sessions, entry.getSessions());
    }

    // Partición frontera: una cartelera puede contener una lista de sesiones vacía
    @Test
    public void constructorStoresEmptySessions() {
        BillboardEntry entry = new BillboardEntry(new Movie(), List.of());

        assertEquals(0, entry.getSessions().size());
    }
}
