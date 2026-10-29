package es.udc.paproject.backend.model.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockTest {

    // Partición equivalente: dos bloques con el mismo contenido y bandera son iguales
    @Test
    public void equalsReturnsTrueForEqualBlocks() {
        Block<String> first = new Block<>(List.of("item"), true);
        Block<String> second = new Block<>(List.of("item"), true);

        assertTrue(first.equals(second));
    }

    // Partición equivalente: un bloque es igual a sí mismo
    @Test
    public void equalsReturnsTrueForSameBlock() {
        Block<String> block = new Block<>(List.of("item"), true);

        assertTrue(block.equals(block));
    }

    // Partición equivalente: bloques iguales tienen el mismo hashCode
    @Test
    public void equalBlocksHaveSameHashCode() {
        Block<String> first = new Block<>(List.of("item"), true);
        Block<String> second = new Block<>(List.of("item"), true);

        assertEquals(first.hashCode(), second.hashCode());
    }

    // Valor frontera: bloques iguales con lista null tienen el mismo hashCode
    @Test
    public void equalBlocksWithNullItemsHaveSameHashCode() {
        Block<String> first = new Block<>(null, false);
        Block<String> second = new Block<>(null, false);

        assertEquals(first.hashCode(), second.hashCode());
    }

    // Partición equivalente: distinto contenido implica bloques distintos
    @Test
    public void equalsReturnsFalseForDifferentItems() {
        Block<String> first = new Block<>(List.of("first"), false);
        Block<String> second = new Block<>(List.of("second"), false);

        assertFalse(first.equals(second));
    }

    // Valor frontera: listas null iguales hacen que los bloques sean iguales
    @Test
    public void equalsReturnsTrueForNullItems() {
        Block<String> first = new Block<>(null, false);
        Block<String> second = new Block<>(null, false);

        assertTrue(first.equals(second));
    }

    // Valor frontera: una lista null y una lista no null hacen distintos los bloques
    @Test
    public void equalsReturnsFalseWhenOnlyOneItemsListIsNull() {
        Block<String> first = new Block<>(null, false);
        Block<String> second = new Block<>(List.of("item"), false);

        assertFalse(first.equals(second));
    }

    // Partición equivalente: distinta bandera implica bloques distintos
    @Test
    public void equalsReturnsFalseForDifferentExistMoreItems() {
        Block<String> first = new Block<>(List.of("item"), false);
        Block<String> second = new Block<>(List.of("item"), true);

        assertFalse(first.equals(second));
    }

    // Valor frontera: null no es igual a un bloque
    @Test
    public void equalsReturnsFalseForNull() {
        Block<String> block = new Block<>(List.of("item"), false);

        assertFalse(block.equals(null));
    }

    // Partición equivalente: un objeto de otra clase no es igual a un bloque
    @Test
    public void equalsReturnsFalseForDifferentClass() {
        Block<String> block = new Block<>(List.of("item"), false);

        assertFalse(block.equals("not a block"));
    }
}
