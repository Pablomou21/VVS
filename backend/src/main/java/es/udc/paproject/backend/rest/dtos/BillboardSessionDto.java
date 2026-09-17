package es.udc.paproject.backend.rest.dtos;


public class BillboardSessionDto {
    private Long id;
    private String date;

    public BillboardSessionDto() {}

    public BillboardSessionDto(Long id, String date) {
        this.id = id;
        this.date = date;
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
}
