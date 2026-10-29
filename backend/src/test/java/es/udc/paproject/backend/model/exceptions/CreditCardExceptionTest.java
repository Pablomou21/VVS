package es.udc.paproject.backend.model.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreditCardExceptionTest {

    // Partición válida: el getter devuelve el identificador de compra recibido
    @Test
    public void getterReturnsOrderId() {
        CreditCardException exception = new CreditCardException(42L, "1234567890123456");

        assertEquals(42L, exception.getOrderId());
    }

    // Partición válida: el getter devuelve el número de tarjeta recibido
    @Test
    public void getterReturnsCreditCardNumber() {
        CreditCardException exception = new CreditCardException(42L, "1234567890123456");

        assertEquals("1234567890123456", exception.getCreditCardNum());
    }
}
