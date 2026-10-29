package es.udc.paproject.backend.model.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SessionAlreadyStartedExceptionTest {

    // Partición válida: el getter devuelve el identificador de sesión recibido
    @Test
    public void getterReturnsSessionId() {
        SessionAlreadyStartedException exception = new SessionAlreadyStartedException(23L);

        assertEquals(23L, exception.getSessionId());
    }
}
