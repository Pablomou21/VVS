package es.udc.paproject.backend.rest.dtos;

import es.udc.paproject.backend.model.entities.Session;

public class SessionConversor {
    public SessionConversor() {
    }

    public final static SessionDto toSessionDto(Session session) {
        return new SessionDto(session.getId(), session.getMovie().getId(), session.getMovie().getTitle(), session.getMovie().getDuration(),
                session.getPrice(), session.getDate().toString(), session.getRoom().getName(), session.getFreeSeats());
    }
}
