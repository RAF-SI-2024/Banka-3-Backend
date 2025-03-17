package rs.raf.stock_service.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status != null) {
            return orderRepository.findByStatus(status);
        }else if (status == null){
            throw new OrderStatusNotFoundException();
        }

        return orderRepository.findAll();

        //slucaj za all?
    }
}
