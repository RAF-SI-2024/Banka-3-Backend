package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class OrderDto {

    private Long id;
    private Long userId;
    private ListingDto listing;
    private OrderType orderType;
    private Integer quantity;
    private Integer contractSize;
    private BigDecimal pricePerUnit;
    private OrderDirection direction;
    private OrderStatus status;
    private Long approvedBy;
    private Boolean isDone;
    private LocalDateTime lastModification;
    private Integer remainingPortions;
    private Boolean afterHours;
}
