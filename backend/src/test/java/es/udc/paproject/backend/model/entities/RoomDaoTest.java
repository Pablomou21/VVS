package es.udc.paproject.backend.model.entities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoomDaoTest {

    @Autowired
    private RoomDao roomDao;

    // guardar una habitacion y comprobar que se le asigna un ID
    @Test
    public void shouldSaveRoomAndGenerateId() {
        Room room = roomDao.save(new Room("Room A", 50));

        assertNotNull(room.getId(), "The database should assign an ID when saving");
    }

    // buscar una habitacion por ID y comprobar que se encuentra
    @Test
    public void shouldFindRoomById() {
        Room savedRoom = roomDao.save(new Room("Room B", 30));
        
        // Fix warning: Ensure ID is not null before using it
        Long id = Objects.requireNonNull(savedRoom.getId());

        Optional<Room> foundRoom = roomDao.findById(id);

        assertTrue(foundRoom.isPresent());
        assertEquals("Room B", foundRoom.get().getName());
        assertEquals(30, foundRoom.get().getCapacity());
    }

    // listar todas las habitaciones 
    @Test
    public void shouldListAllRooms() {
        Room room1 = roomDao.save(new Room("Room 1", 20));
        Room room2 = roomDao.save(new Room("Room 2", 40));

        // Fix warning: Ensure IDs are not null
        Long id1 = Objects.requireNonNull(room1.getId());
        Long id2 = Objects.requireNonNull(room2.getId());

        Iterable<Room> allRooms = roomDao.findAll();

        boolean containsRoom1 = StreamSupport.stream(allRooms.spliterator(), false)
                .anyMatch(r -> id1.equals(r.getId()));
        boolean containsRoom2 = StreamSupport.stream(allRooms.spliterator(), false)
                .anyMatch(r -> id2.equals(r.getId()));

        assertTrue(containsRoom1);
        assertTrue(containsRoom2);
    }

    // actualizar una habitacion 
    @Test
    public void shouldUpdateRoomData() {
        Room room = roomDao.save(new Room("Original", 100));
        
        Long id = Objects.requireNonNull(room.getId());

        room.setName("Modified");
        room.setCapacity(150);
        roomDao.save(room);

        Room updatedRoom = roomDao.findById(id).orElseThrow();
        assertEquals("Modified", updatedRoom.getName());
        assertEquals(150, updatedRoom.getCapacity());
    }

    // borrar una habitacion y comprobar que ya no se encuentra
    @Test
    public void shouldDeleteRoomById() {
        Room room = roomDao.save(new Room("To delete", 10));
        
        Long id = Objects.requireNonNull(room.getId());

        roomDao.deleteById(id);

        assertFalse(roomDao.existsById(id), "The room should not exist in the DB after being deleted");
    }
}