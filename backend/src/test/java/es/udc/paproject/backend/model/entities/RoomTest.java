package es.udc.paproject.backend.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

// Pruebas de Room con jqwik: cada @Property se ejecuta con datos generados del rango indicado.
public class RoomTest {

    // Partición equivalente: el constructor conserva nombres no vacíos
    @Property
    public void constructorStoresName(
            @ForAll @StringLength(min = 1, max = 50) String name) {

        Room room = new Room(name, 100);

        assertEquals(name, room.getName());
    }

    // Partición equivalente: el constructor conserva capacidades positivas
    @Property
    public void constructorStoresCapacity(
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

        Room room = new Room("name", capacity);

        assertEquals(capacity, room.getCapacity());
    }

    // Partición equivalente: setName sustituye el nombre previo
    @Property
    public void setNameUpdatesName(
            @ForAll @StringLength(min = 1, max = 50) String name) {

        // La sala sin nombre (null) garantiza que el dato generado sustituye el nombre
        Room room = new Room();
        room.setName(name);

        assertEquals(name, room.getName());
    }

    // Partición equivalente: setCapacity sustituye la capacidad previa
    @Property
    public void setCapacityUpdatesCapacity(
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

        // La capacidad inicial queda fuera del rango generado para garantizar el cambio
        Room room = new Room("name", 1000);
        room.setCapacity(capacity);

        assertEquals(capacity, room.getCapacity());
    }

    // Caso fijo: el identificador se asigna al persistir la sala
    @Example
    public void newRoomHasNoId() {

        Room room = new Room();

        assertNull(room.getId());
    }

    // Dato aleatorio: se puede asignar y recuperar el identificador
    @Property
    public void setIdUpdatesId(@ForAll @LongRange(min = 1, max = 1000000) long id) {
        Room room = new Room();
        room.setId(id);

        assertEquals(id, room.getId());
    }

    // Partición inválida (falla, issue #2): capacidad entre -100 y 0, incluido el valor frontera 0
    @Property
    public void roomWithNonPositiveCapacityIsRejected(
            @ForAll @IntRange(min = -100, max = 0) int capacity) {

        assertThrows(IllegalArgumentException.class, () -> new Room("name", capacity));
    }

    // Partición inválida (falla, issue #3): nombre vacío, valor frontera de longitud 0
    @Example
    public void roomWithEmptyNameIsRejected() {

        assertThrows(IllegalArgumentException.class, () -> new Room("", 100));
    }
}
