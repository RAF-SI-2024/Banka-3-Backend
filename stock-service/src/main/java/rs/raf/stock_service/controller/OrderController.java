package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Get orders with optional filtering",
            description = "Returns a list of orders. Only supervisors can access this endpoint. Orders can be filtered by status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved orders"),
            @ApiResponse(responseCode = "403", description = "Access denied – only supervisors can view orders"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestParam OrderStatus status,
            Pageable pageable) {

        Page<Order> ordersPage = orderService.getOrdersByStatus(status, pageable);
        Page<OrderDto> dtoPage = ordersPage.map(OrderDto::new);

        return ResponseEntity.ok(dtoPage);
    }
}