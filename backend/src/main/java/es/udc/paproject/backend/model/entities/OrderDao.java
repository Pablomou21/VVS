package es.udc.paproject.backend.model.entities;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.CrudRepository;

public interface OrderDao extends CrudRepository<Order, Long> {
    // Find orders for a user
    Slice<Order> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);
}
