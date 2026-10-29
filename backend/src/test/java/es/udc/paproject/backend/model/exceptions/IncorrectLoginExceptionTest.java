package es.udc.paproject.backend.model.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IncorrectLoginExceptionTest {

    // Partición válida: el getter devuelve el nombre de usuario recibido
    @Test
    public void getterReturnsUserName() {
        IncorrectLoginException exception = new IncorrectLoginException("iago", "password");

        assertEquals("iago", exception.getUserName());
    }

    // Partición válida: el getter devuelve la contraseña recibida
    @Test
    public void getterReturnsPassword() {
        IncorrectLoginException exception = new IncorrectLoginException("iago", "password");

        assertEquals("password", exception.getPassword());
    }
}
