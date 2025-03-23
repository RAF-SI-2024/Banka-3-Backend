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
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.service.OrderService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetOrdersByStatus_WhenStatusIsProvided() {
        Order order1 = Order.builder()
                .status(OrderStatus.APPROVED)
                .quantity(10)
                .pricePerUnit(new BigDecimal("150.50"))
                .lastModification(LocalDateTime.now())
                .build();

        Order order2 = Order.builder()
                .status(OrderStatus.APPROVED)
                .quantity(5)
                .pricePerUnit(new BigDecimal("200.00"))
                .lastModification(LocalDateTime.now().minusDays(1))
                .build();

        List<Order> orderList = Arrays.asList(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(orderRepository.findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class)))
                .thenReturn(orderPage);

        Page<OrderDto> result = orderService.getOrdersByStatus(OrderStatus.APPROVED, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(OrderStatus.APPROVED, result.getContent().get(0).getStatus());
        verify(orderRepository, times(1)).findByStatus(eq(OrderStatus.APPROVED), any(PageRequest.class));
    }

    @Test
    void testGetOrdersByStatus_WhenStatusIsNull() {
        Order order1 = Order.builder()
                .status(OrderStatus.PENDING)
                .quantity(8)
                .pricePerUnit(new BigDecimal("120.00"))
                .lastModification(LocalDateTime.now())
                .build();

        Order order2 = Order.builder()
                .status(OrderStatus.APPROVED)
                .quantity(15)
                .pricePerUnit(new BigDecimal("180.00"))
                .lastModification(LocalDateTime.now().minusHours(3))
                .build();

        List<Order> orderList = Arrays.asList(order1, order2);
        Page<Order> orderPage = new PageImpl<>(orderList);

        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);

        Page<OrderDto> result = orderService.getOrdersByStatus(null, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        verify(orderRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void approveOrder_ShouldThrowOrderNotFoundException_WhenOrderDoesNotExist() {
        Long orderId = 1L;
        String authHeader = "Bearer test-token";

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderService.approveOrder(orderId, authHeader);
        });

        assertEquals("Order with ID "+orderId+" not found.", exception.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(jwtTokenUtil, never()).getUserIdFromAuthHeader(anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }
}
