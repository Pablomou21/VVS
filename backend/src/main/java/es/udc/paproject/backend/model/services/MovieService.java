package es.udc.paproject.backend.model.services;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.exceptions.DayOutOfRangeException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;

import java.util.List;

public interface MovieService {

    // View billboard
    // 0=today, 1=tomorrow, ...
    List<BillboardEntry> viewBillboard(int dayOffset) throws DayOutOfRangeException;

    // View movie details
    Movie findMovieById(Long movieId)
            throws InstanceNotFoundException;

    // View session details
    Session findSessionById(Long sessionId)
            throws InstanceNotFoundException, SessionAlreadyStartedException;
}
