package rs.raf.stock_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    List<Order> findByIsDoneAndStatusAndOrderType(boolean isDone, OrderStatus orderStatus, OrderType orderType);
    List<Order> findAllByUserId(Long userId);
    List<Order> findAllByDirection(OrderDirection orderDirection);

}

