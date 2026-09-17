package es.udc.paproject.backend.rest.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BuyTicketsParamsDto {

    private Long sessionId;
    private Integer numTickets;
    private String creditCardNum;

    public BuyTicketsParamsDto() {
    }

    @NotNull
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    @NotNull
    @Min(1)
    @Max(10)
    public Integer getNumTickets() {
        return numTickets;
    }

    public void setNumTickets(Integer numTickets) {
        this.numTickets = numTickets;
    }

    @NotBlank
    @Size(min=1, max = 16)
    public String getCreditCardNum() {
        return creditCardNum;
    }

    public void setCreditCardNum(String creditCardNum) {
        this.creditCardNum = creditCardNum;
    }
}