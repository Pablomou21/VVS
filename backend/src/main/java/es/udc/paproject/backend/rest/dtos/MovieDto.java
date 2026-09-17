package es.udc.paproject.backend.rest.dtos;

public class MovieDto {
    private Long id;
    private String title;
    private String summary;
    private short duration;

    private MovieDto() {
    }

    public MovieDto(Long id, String title, String summary, short duration) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.duration = duration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public short getDuration() {
        return duration;
    }

    public void setDuration(short duration) {
        this.duration = duration;
    }
}
