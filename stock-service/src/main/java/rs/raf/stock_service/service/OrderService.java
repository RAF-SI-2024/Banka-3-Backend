package rs.raf.stock_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.exceptions.OrderStatusNotFoundException;
import rs.raf.stock_service.repository.OrderRepository;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        if (status == null) {
            return orderRepository.findAll(pageable);
            // vraca paginirano sve
        }
        return orderRepository.findByStatus(status, pageable);
    }
}
