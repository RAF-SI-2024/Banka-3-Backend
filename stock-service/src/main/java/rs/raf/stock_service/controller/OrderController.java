package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.CreateOrderDto;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.dto.TaxGetResponseDto;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.service.OrderService;

import javax.validation.Valid;
import javax.websocket.server.PathParam;


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
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN') or hasRole('AGENT')")
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/approve/{id}")
    @Operation(summary = "Approve order.", description = "Approves order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order approved successfully."),
            @ApiResponse(responseCode = "404", description = "Order not found.")
    })
    public ResponseEntity<?> approveOrder(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        try {
            orderService.approveOrder(id, authHeader);
            return ResponseEntity.ok().build();
        } catch (OrderNotFoundException | CantApproveNonPendingOrder e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/decline/{id}")
    @Operation(summary = "Decline order.", description = "Declines order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order declined successfully."),
            @ApiResponse(responseCode = "404", description = "Order not found.")
    })
    public ResponseEntity<?> decline(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        try {
            orderService.declineOrder(id, authHeader);
            return ResponseEntity.ok().build();
        } catch (OrderNotFoundException | CantApproveNonPendingOrder e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('AGENT') or hasRole('CLIENT')")
    @PostMapping
    @Operation(summary = "Create order.", description = "Creates a new order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ResponseEntity<?> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateOrderDto createOrderDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(createOrderDto, authHeader));
        } catch (ListingNotFoundException | AccountNotFoundException | ActuaryLimitNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (StopPriceMissingException | LimitPriceMissingException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping("/tax")
    @Operation(summary = "Process taxes.", description = "Pays taxes where possible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
    })
    public ResponseEntity<?> processTaxes() {
        try {
            orderService.processTaxes();
            return ResponseEntity.status(HttpStatus.OK).body("Taxes processed successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
