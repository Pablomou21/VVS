package es.udc.paproject.backend.model.services;

import java.util.List;
import java.util.Optional;

import es.udc.paproject.backend.model.entities.MovieDao;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.exceptions.DayOutOfRangeException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

@Service
@Transactional(readOnly=true)
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private SessionDao sessionDao;

    @Override
    public List<BillboardEntry> viewBillboard(int dayOffset) throws DayOutOfRangeException {
        if (dayOffset < 0 || dayOffset > 6) {
            throw new DayOutOfRangeException();
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        if (dayOffset == 0) {
            startTime = LocalDateTime.now();
        } else {
            startTime = LocalDate.now().plusDays(dayOffset).atStartOfDay();
        }
        endTime = LocalDate.now().plusDays(dayOffset).atTime(LocalTime.MAX);

        List<Session> sessions = sessionDao.findByDateBetweenOrderByMovieTitleAscDateAsc(startTime, endTime);

        List<BillboardEntry> billboardEntries = new ArrayList<>();

        if (sessions.isEmpty()) {
            return billboardEntries;
        }

        List<Session> movieSessions = new ArrayList<>();
        Movie currentMovie = sessions.getFirst().getMovie();

        for (Session session : sessions) {
            if (!session.getMovie().getId().equals(currentMovie.getId())) {
                billboardEntries.add(new BillboardEntry(currentMovie, movieSessions));
                currentMovie = session.getMovie();
                movieSessions = new ArrayList<>();
            }
            movieSessions.add(session);
        }

        if (!movieSessions.isEmpty()) {
            billboardEntries.add(new BillboardEntry(currentMovie, movieSessions));
        }
        return billboardEntries;
    }

    @Override
    public Movie findMovieById(Long movieId) throws InstanceNotFoundException {
        Optional<Movie> movie  = movieDao.findById(movieId);

        if (movie.isEmpty()){
            throw new InstanceNotFoundException("project.entities.movie", movieId);
        }

        return movie.get();
    }

    @Override
    public Session findSessionById(Long sessionId) throws InstanceNotFoundException, SessionAlreadyStartedException {

        Optional<Session> session  = sessionDao.findById(sessionId);

        if (session.isEmpty()){
            throw new InstanceNotFoundException("project.entities.session", sessionId);
        }

        if (session.get().getDate().isBefore(LocalDateTime.now())){
            throw new SessionAlreadyStartedException(sessionId);
        }

        return session.get();

    }
}
