package es.udc.paproject.backend.model.entities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class MovieDaoTest {

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private TestEntityManager entityManager;

    // Partición válida: una película guardada recibe un identificador de la base de datos
    @Test
    public void saveAssignsId() {
        Movie movie = movieDao.save(new Movie("Movie", "Summary", (short) 120));

        assertNotNull(movie.getId());
    }

    // Partición válida: buscar por el identificador recupera el título guardado
    @Test
    public void findByIdReturnsSavedTitle() {
        Movie movie = movieDao.save(new Movie("Saved title", "Summary", (short) 120));
        entityManager.flush();
        entityManager.clear();

        assertEquals("Saved title", movieDao.findById(movie.getId()).orElseThrow().getTitle());
    }

    // Partición inválida: un identificador inexistente no encuentra película
    @Test
    public void findByIdReturnsEmptyForUnknownId() {
        assertTrue(movieDao.findById(Long.MAX_VALUE).isEmpty());
    }

    // Partición válida: findAll incluye las películas guardadas
    @Test
    public void findAllIncludesSavedMovie() {
        Movie movie = movieDao.save(new Movie("Listed movie", "Summary", (short) 120));
        entityManager.flush();
        entityManager.clear();

        assertTrue(StreamSupport.stream(movieDao.findAll().spliterator(), false)
                .anyMatch(foundMovie -> Objects.equals(foundMovie.getId(), movie.getId())));
    }

    // Partición válida: guardar una película modificada persiste el nuevo resumen
    @Test
    public void saveUpdatesMovieSummary() {
        Movie movie = movieDao.save(new Movie("Movie", "Original summary", (short) 120));
        movie.setSummary("Updated summary");
        movieDao.save(movie);
        entityManager.flush();
        entityManager.clear();

        assertEquals("Updated summary", movieDao.findById(movie.getId()).orElseThrow().getSummary());
    }

    // Partición válida: borrar una película por su identificador la elimina
    @Test
    public void deleteByIdRemovesMovie() {
        Movie movie = movieDao.save(new Movie("Movie", "Summary", (short) 120));
        Long movieId = Objects.requireNonNull(movie.getId());
        movieDao.deleteById(movieId);
        entityManager.flush();
        entityManager.clear();

        assertFalse(movieDao.existsById(movieId));
    }
}
