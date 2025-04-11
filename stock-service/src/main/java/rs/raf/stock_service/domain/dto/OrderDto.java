package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
    private BigDecimal stopPrice;
    private boolean stopFulfilled;
    private Boolean afterHours;
    private List<TransactionDto> transactions;
    private BigDecimal profit;
}
