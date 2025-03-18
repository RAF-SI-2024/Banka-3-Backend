package unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.exceptions.OrderStatusNotFoundException;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.OrderService;

import java.util.Arrays;
import java.util.List;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetOrdersByStatus_WhenStatusIsProvided() {
        Order order1 = new Order();
        order1.setStatus(OrderStatus.APPROVED);
        Order order2 = new Order();
        order2.setStatus(OrderStatus.APPROVED);
        List<Order> orderList = Arrays.asList(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(orderRepository.findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class)))
                .thenReturn(orderPage);

        Page<Order> result = orderService.getOrdersByStatus(OrderStatus.APPROVED, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(OrderStatus.APPROVED, result.getContent().get(0).getStatus());
        verify(orderRepository, times(1)).findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class));
    }

    @Test
    void testGetOrdersByStatus_WhenStatusIsNull() {
        Order order1 = new Order();
        Order order2 = new Order();
        List<Order> orderList = Arrays.asList(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);

        Page<Order> result = orderService.getOrdersByStatus(null, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(PageRequest.class));
    }
}

