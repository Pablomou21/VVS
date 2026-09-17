package es.udc.paproject.backend.model.services;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Session;

import java.util.List;

public class BillboardEntry {
    private Movie movie;
    private List<Session> sessions;

    public BillboardEntry(Movie movie, List<Session> sessions) {
        this.movie = movie;
        this.sessions = sessions;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }
}
