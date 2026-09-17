package es.udc.paproject.backend.model.exceptions;

public class SessionAlreadyStartedException extends Exception {
    private Long sessionId;

    public SessionAlreadyStartedException(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSessionId() {
        return sessionId;
    }
}