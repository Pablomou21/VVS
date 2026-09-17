package es.udc.paproject.backend.model.entities;

import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionDao extends CrudRepository<Session, Long> {
    // Find sessions between a range of time
    List<Session> findByDateBetweenOrderByMovieTitleAscDateAsc(LocalDateTime startDate, LocalDateTime endDate);
}
