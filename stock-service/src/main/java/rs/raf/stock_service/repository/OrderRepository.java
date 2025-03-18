package rs.raf.stock_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}

