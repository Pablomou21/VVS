package es.udc.paproject.backend.model.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstanceExceptionTest {

    // Partición válida: la subclase concreta conserva el nombre recibido por InstanceException
    @Test
    public void getterReturnsName() {
        InstanceException exception = new InstanceNotFoundException("project.entities.movie", 17L);

        assertEquals("project.entities.movie", exception.getName());
    }

    // Partición válida: la subclase concreta conserva la clave recibida por InstanceException
    @Test
    public void getterReturnsKey() {
        InstanceException exception = new InstanceNotFoundException("project.entities.movie", 17L);

        assertEquals(17L, exception.getKey());
    }
}
