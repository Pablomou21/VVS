package es.udc.paproject.backend.rest.controllers;

import static es.udc.paproject.backend.rest.dtos.BillboardEntryConversor.toBillboardEntryDtos;
import static es.udc.paproject.backend.rest.dtos.MovieConversor.toMovieDto;
import static es.udc.paproject.backend.rest.dtos.SessionConversor.toSessionDto;

import java.util.List;
import java.util.Locale;

import es.udc.paproject.backend.model.exceptions.DayOutOfRangeException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import es.udc.paproject.backend.model.services.BillboardEntry;
import es.udc.paproject.backend.rest.common.ErrorsDto;
import es.udc.paproject.backend.rest.dtos.BillboardEntryDto;
import es.udc.paproject.backend.rest.dtos.SessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.services.MovieService;
import es.udc.paproject.backend.rest.dtos.MovieDto;


@RestController
@RequestMapping("/movies")
public class MovieController {

    private final static String DAY_OUT_OF_RANGE_EXCEPTION_CODE = "project.exceptions.DayOutOfRangeException";

    @Autowired
    private MovieService movieService;

    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(DayOutOfRangeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorsDto handleDayOutOfRangeException(DayOutOfRangeException exception, Locale locale) {
        String errorMessage = messageSource.getMessage(DAY_OUT_OF_RANGE_EXCEPTION_CODE,
                null, DAY_OUT_OF_RANGE_EXCEPTION_CODE, locale);
        return new ErrorsDto(errorMessage);
    }

    @GetMapping("/{id}")
    public MovieDto findMovieById(@PathVariable Long id) throws InstanceNotFoundException {
        Movie movie = movieService.findMovieById(id);
        return toMovieDto(movie);
    }

    @GetMapping("/sessions/{id}")
    public SessionDto findSessionById(@PathVariable Long id) throws InstanceNotFoundException, SessionAlreadyStartedException {
        Session session = movieService.findSessionById(id);
        return toSessionDto(session);
    }

    @GetMapping("/billboard/{day}")
    public List<BillboardEntryDto> viewBillboard(@PathVariable int day) throws DayOutOfRangeException{
        List<BillboardEntry> billboard = movieService.viewBillboard(day);
        return toBillboardEntryDtos(billboard);
    }
}
