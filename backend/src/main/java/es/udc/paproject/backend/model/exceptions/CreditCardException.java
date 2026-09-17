package es.udc.paproject.backend.model.exceptions;

public class CreditCardException extends Exception {
    private Long orderId;
    private String creditCardNum;

    public CreditCardException(Long orderId, String creditCardNum) {
        this.orderId = orderId;
        this.creditCardNum = creditCardNum;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCreditCardNum() {
        return creditCardNum;
    }
}
