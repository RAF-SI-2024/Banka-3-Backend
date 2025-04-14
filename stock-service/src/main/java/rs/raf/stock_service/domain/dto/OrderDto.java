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
    private String clientName;
    private ListingDto listing;
    private String accountNumber;
    private OrderType orderType;
    private OrderDirection direction;
    private Integer contractSize;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalPrice;
    private BigDecimal commission;
    private OrderStatus status;
    private Long approvedBy;
    private Boolean isDone;
    private LocalDateTime lastModification;
    private Integer remainingPortions;
    private BigDecimal stopPrice;
    private boolean stopFulfilled;
    private Boolean afterHours;
    private BigDecimal profit;
    private List<TransactionDto> transactions;
}
