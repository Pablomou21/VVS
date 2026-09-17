package es.udc.paproject.backend.rest.dtos;

import java.math.BigDecimal;

public class OrderDto {

    private Long id;
    private String date;
    private String movieTitle;
    private int units;
    private BigDecimal totalPrice;
    private String sessionDate;
    private boolean delivered;

    public OrderDto() {
    }

    public OrderDto(Long id, String date, String movieTitle, int units, BigDecimal totalPrice,
                    String sessionDate, boolean delivered) {
        this.id = id;
        this.date = date;
        this.movieTitle = movieTitle;
        this.units = units;
        this.totalPrice = totalPrice;
        this.sessionDate = sessionDate;
        this.delivered = delivered;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }
}