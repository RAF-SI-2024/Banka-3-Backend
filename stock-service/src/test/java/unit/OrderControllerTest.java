package unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.stock_service.controller.OrderController;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.service.OrderService;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void approveOrder_ShouldReturnOk_WhenOrderIsApprovedSuccessfully() {
        Long orderId = 1L;
        String authHeader = "Bearer test-token";

        ResponseEntity<?> response = orderController.approveOrder(authHeader, orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderService).approveOrder(eq(orderId), eq(authHeader));
    }

    @Test
    void approveOrder_ShouldReturnNotFound_WhenOrderNotFound() {
        Long orderId = 1L;
        String authHeader = "Bearer test-token";

        doThrow(new OrderNotFoundException(orderId))
                .when(orderService).approveOrder(eq(orderId), eq(authHeader));

        ResponseEntity<?> response = orderController.approveOrder(authHeader, orderId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("not found"));
        verify(orderService).approveOrder(eq(orderId), eq(authHeader));
    }
    @Test
    void declineOrder_ShouldReturnOk_WhenOrderIsDeclinedSuccessfully() {
        Long orderId = 1L;
        String authHeader = "Bearer test-token";

        ResponseEntity<?> response = orderController.decline(authHeader, orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderService).declineOrder(eq(orderId), eq(authHeader));
    }

    @Test
    void declineOrder_ShouldReturnNotFound_WhenOrderNotFound() {
        Long orderId = 1L;
        String authHeader = "Bearer test-token";

        doThrow(new OrderNotFoundException(orderId))
                .when(orderService).declineOrder(eq(orderId), eq(authHeader));

        ResponseEntity<?> response = orderController.decline(authHeader, orderId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("not found"));
        verify(orderService).declineOrder(eq(orderId), eq(authHeader));
    }
}