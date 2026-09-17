package es.udc.paproject.backend.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeliverOrderParamsDto {
    private String creditCardNum;

    @NotBlank
    @Size(min=1, max = 16)
    public String getCreditCardNum() {
        return creditCardNum;
    }

    public void setCreditCardNum(String creditCardNum) {
        this.creditCardNum = creditCardNum;
    }
}
