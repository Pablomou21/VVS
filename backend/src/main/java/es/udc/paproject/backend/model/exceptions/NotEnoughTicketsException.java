package es.udc.paproject.backend.model.exceptions;

public class NotEnoughTicketsException extends Exception {

    private int numberTicketsLeft;

    public NotEnoughTicketsException(int numberTicketsLeft) {
        this.numberTicketsLeft = numberTicketsLeft;
    }

    public int getNumberTicketsLeft() {
        return numberTicketsLeft;
    }
}
