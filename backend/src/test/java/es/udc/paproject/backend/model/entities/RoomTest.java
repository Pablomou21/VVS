package es.udc.paproject.backend.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

// Pruebas de Room con jqwik: cada @Property se ejecuta hasta 1000 veces con datos aleatorios;
// si el rango tiene menos de 1000 valores, jqwik los prueba todos (generación exhaustiva)
public class RoomTest {

    // ---------- Partición válida (deben pasar) ----------

    // El constructor guarda el nombre recibido (nombres de 1 a 50 caracteres)
    @Property
    public void constructorStoresName(
            @ForAll @StringLength(min = 1, max = 50) String name) {

        Room room = new Room(name, 100);

        assertEquals(name, room.getName());
    }

    // El constructor guarda la capacidad recibida (capacidades de 1 a 500)
    @Property
    public void constructorStoresCapacity(
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

        Room room = new Room("name", capacity);

        assertEquals(capacity, room.getCapacity());
    }

    // setName sustituye el nombre por el recibido (nombres de 1 a 50 caracteres)
    @Property
    public void setNameUpdatesName(
            @ForAll @StringLength(min = 1, max = 50) String name) {

        // Nombre inicial vacío: jqwik nunca lo genera (min = 1), el setter siempre cambia el valor
        Room room = new Room("", 100);
        room.setName(name);

        assertEquals(name, room.getName());
    }

    // setCapacity sustituye la capacidad por la recibida (capacidades de 1 a 500)
    @Property
    public void setCapacityUpdatesCapacity(
            @ForAll @IntRange(min = 1, max = 500) int capacity) {

        // Capacidad inicial 1000: jqwik nunca la genera (max = 500), el setter siempre la cambia
        Room room = new Room("name", 1000);
        room.setCapacity(capacity);

        assertEquals(capacity, room.getCapacity());
    }

    // Una sala sin guardar no tiene id: lo asigna la base de datos al guardarla (caso fijo)
    @Example
    public void newRoomHasNoId() {

        Room room = new Room();

        assertNull(room.getId());
    }

    // ---------- Partición inválida (fallarán: el código no valida) ----------

    // Una sala con capacidad 0 o negativa es inválida (frontera 0, de -100 a 0)
    @Property
    public void roomWithNonPositiveCapacityIsRejected(
            @ForAll @IntRange(min = -100, max = 0) int capacity) {

        assertThrows(IllegalArgumentException.class, () -> new Room("name", capacity));
    }

    // Una sala con nombre vacío es inválida (caso fijo: frontera de longitud 0)
    @Example
    public void roomWithEmptyNameIsRejected() {

        assertThrows(IllegalArgumentException.class, () -> new Room("", 100));
    }
}
