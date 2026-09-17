package es.udc.paproject.backend.rest.dtos;

import java.math.BigDecimal;

public class SessionDto {

    private Long id;
    private Long movieId;
    private String movieTitle;
    private short movieDuration;
    private BigDecimal price;
    private String date;
    private String roomName;
    private int freeSeats;

    public SessionDto() {
    }

    public SessionDto(Long id, Long movieId, String movieTitle, short movieDuration, BigDecimal price, String date, String roomName, int freeSeats) {
        this.id = id;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.movieDuration = movieDuration;
        this.price = price;
        this.date = date;
        this.roomName = roomName;
        this.freeSeats = freeSeats;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId){
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public short getMovieDuration() {
        return movieDuration;
    }

    public void setMovieDuration(short movieDuration) {
        this.movieDuration = movieDuration;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getFreeSeats() {
        return freeSeats;
    }

    public void setFreeSeats(int freeSeats) {
        this.freeSeats = freeSeats;
    }
}
