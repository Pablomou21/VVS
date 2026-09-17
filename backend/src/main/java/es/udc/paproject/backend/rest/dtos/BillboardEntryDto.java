package es.udc.paproject.backend.rest.dtos;

import java.util.List;

public class BillboardEntryDto {
    private Long movieId;
    private String movieTitle;
    private List<BillboardSessionDto> sessions;

    public BillboardEntryDto() {}

    public BillboardEntryDto(Long movieId, String movieTitle, List<BillboardSessionDto> sessions) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.sessions = sessions;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public List<BillboardSessionDto> getSessions() {
        return sessions;
    }

    public void setSessions(List<BillboardSessionDto> sessions) {
        this.sessions = sessions;
    }
}
