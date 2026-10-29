package es.udc.paproject.backend.model.entities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class RoomDaoTest {

    @Autowired
    private RoomDao roomDao;

    private boolean containsRoomId(Iterable<Room> rooms, Long id) {
        return StreamSupport.stream(rooms.spliterator(), false)
                .anyMatch(room -> id.equals(room.getId()));
    }

    // Partición equivalente: guardar una habitación válida debe asignarle un ID
    @Test
    public void shouldSaveRoomAndGenerateId() {
        Room room = roomDao.save(new Room("Room A", 50));

        assertNotNull(room.getId(), "The database should assign an ID when saving");
    }

    // Partición equivalente: buscar por el ID de una habitación guardada
    @Test
    public void shouldFindRoomById() {
        Room savedRoom = roomDao.save(new Room("Room B", 30));
        Long id = Objects.requireNonNull(savedRoom.getId());

        Optional<Room> foundRoom = roomDao.findById(id);

        assertTrue(foundRoom.isPresent());
    }

    // Partición equivalente: recuperar el nombre de una habitación guardada
    @Test
    public void shouldFindRoomByIdWithName() {
        Room savedRoom = roomDao.save(new Room("Room B", 30));
        Long id = Objects.requireNonNull(savedRoom.getId());

        assertEquals("Room B", roomDao.findById(id).orElseThrow().getName());
    }

    // Partición equivalente: recuperar la capacidad de una habitación guardada
    @Test
    public void shouldFindRoomByIdWithCapacity() {
        Room savedRoom = roomDao.save(new Room("Room B", 30));
        Long id = Objects.requireNonNull(savedRoom.getId());

        assertEquals(30, roomDao.findById(id).orElseThrow().getCapacity());
    }

    // Partición equivalente: incluir una habitación guardada en el listado
    @Test
    public void shouldListFirstRoom() {
        Room room1 = roomDao.save(new Room("Room 1", 20));
        Long id1 = Objects.requireNonNull(room1.getId());

        assertTrue(containsRoomId(roomDao.findAll(), id1));
    }

    // Partición equivalente: incluir otra habitación guardada en el listado
    @Test
    public void shouldListSecondRoom() {
        roomDao.save(new Room("Room 1", 20));
        Room room2 = roomDao.save(new Room("Room 2", 40));
        Long id2 = Objects.requireNonNull(room2.getId());

        assertTrue(containsRoomId(roomDao.findAll(), id2));
    }

    // Partición equivalente: persistir el nombre actualizado
    @Test
    public void shouldUpdateRoomName() {
        Room room = roomDao.save(new Room("Original", 100));
        Long id = Objects.requireNonNull(room.getId());
        room.setName("Modified");
        roomDao.save(room);

        assertEquals("Modified", roomDao.findById(id).orElseThrow().getName());
    }

    // Partición equivalente: persistir la capacidad actualizada
    @Test
    public void shouldUpdateRoomCapacity() {
        Room room = roomDao.save(new Room("Original", 100));
        Long id = Objects.requireNonNull(room.getId());
        room.setCapacity(150);
        roomDao.save(room);

        assertEquals(150, roomDao.findById(id).orElseThrow().getCapacity());
    }

    // Partición equivalente: eliminar una habitación guardada
    @Test
    public void shouldDeleteRoomById() {
        Room room = roomDao.save(new Room("To delete", 10));
        
        Long id = Objects.requireNonNull(room.getId());

        roomDao.deleteById(id);

        assertFalse(roomDao.existsById(id), "The room should not exist in the DB after being deleted");
    }
}