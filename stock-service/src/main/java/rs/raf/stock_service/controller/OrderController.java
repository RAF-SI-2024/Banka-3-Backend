package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders") //da nece praviti problem?
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
    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }
}