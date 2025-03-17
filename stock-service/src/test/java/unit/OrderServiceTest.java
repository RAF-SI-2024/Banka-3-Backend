package unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

        when(orderRepository.findByStatus(OrderStatus.APPROVED))
                .thenReturn(Arrays.asList(order1, order2));

        List<Order> result = orderService.getOrdersByStatus(OrderStatus.APPROVED);

        assertEquals(2, result.size());
        assertEquals(OrderStatus.APPROVED, result.get(0).getStatus());
        verify(orderRepository, times(1)).findByStatus(OrderStatus.APPROVED);
    }


    @Test
    void testGetOrdersByStatus_WhenStatusIsNull() {
        Exception exception = assertThrows(OrderStatusNotFoundException.class, () -> {
            orderService.getOrdersByStatus(null);
        });

        assertEquals("Status not found.", exception.getMessage());
    }

    @Test
    void testGetOrdersByStatus_WhenDatabaseErrorOccurs() {
        when(orderRepository.findByStatus(OrderStatus.APPROVED))
                .thenThrow(new RuntimeException("Database error"));


        Exception exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrdersByStatus(OrderStatus.APPROVED);
        });

        assertEquals("Database error", exception.getMessage());
    }
}
