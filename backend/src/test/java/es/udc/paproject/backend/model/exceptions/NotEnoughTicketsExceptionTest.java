package es.udc.paproject.backend.model.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotEnoughTicketsExceptionTest {

    // Partición válida: el getter devuelve las plazas disponibles recibidas
    @Test
    public void getterReturnsNumberTicketsLeft() {
        NotEnoughTicketsException exception = new NotEnoughTicketsException(7);

        assertEquals(7, exception.getNumberTicketsLeft());
    }
}
