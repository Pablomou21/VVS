package es.udc.paproject;

import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.RoomDao;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class RoomDaoTest {

    @Autowired
    private RoomDao roomDao;

    @Property(tries = 20)
    public void roomDaoMaintainsCrudOperations(
            @ForAll @StringLength(min = 1, max = 50) String name,
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

            // guarda una sala nueva.
        Room room = roomDao.save(new Room(name, capacity));

            // comprueba que la sala tiene identificador.
        assertNotNull(room.getId());
        Long roomId = Objects.requireNonNull(room.getId());

            // busca la sala por su identificador.
        Optional<Room> storedRoom = roomDao.findById(roomId);

        assertEquals(Optional.of(room), storedRoom);
        assertEquals(name, storedRoom.orElseThrow().getName());
        assertEquals(capacity, storedRoom.orElseThrow().getCapacity());

            // comprueba que la sala aparece en todas las salas.
            assertTrue(StreamSupport.stream(roomDao.findAll().spliterator(), false)
                .anyMatch(candidate -> roomId.equals(candidate.getId())));

            // actualiza los datos de la sala.
        String updatedName = name + " updated";
        int updatedCapacity = capacity + 1;

        room.setName(updatedName);
        room.setCapacity(updatedCapacity);
        roomDao.save(room);

        Room updatedRoom = roomDao.findById(roomId).orElseThrow();

        assertEquals(updatedName, updatedRoom.getName());
        assertEquals(updatedCapacity, updatedRoom.getCapacity());

        // elimina la sala por su identificador.
        roomDao.deleteById(roomId);

        // comprueba que la sala ya no existe.
        assertFalse(roomDao.existsById(roomId));
    }
}