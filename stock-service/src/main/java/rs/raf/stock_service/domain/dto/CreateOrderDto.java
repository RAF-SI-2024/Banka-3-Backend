package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderType;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderDto {

    @NotNull(message = "Listing id cannot be null")
    private Long listingId;

    @NotNull(message = "Order type cannot be null")
    private OrderType orderType;

    @NotNull(message = "Quantity cannot be null")
    private Integer quantity;

    @NotNull(message = "Contract size cannot be null")
    private Integer contractSize;

    @NotNull(message = "Order direction cannot be null")
    private OrderDirection orderDirection;

    @NotNull(message = "Account number cannot be null")
    private String accountNumber;

    @NotNull(message = "All or None cannot be null")
    private boolean allOrNone;

    private BigDecimal limitPrice;
    private BigDecimal stopPrice;

    public CreateOrderDto(Long listingId, OrderType orderType, Integer quantity, Integer contractSize, OrderDirection orderDirection,
                          String accountNumber, boolean allOrNone){
        this.listingId = listingId;
        this.orderType = orderType;
        this.quantity = quantity;
        this.contractSize = contractSize;
        this.orderDirection = orderDirection;
        this.accountNumber = accountNumber;
        this.allOrNone = allOrNone;
    }

    public CreateOrderDto(Long listingId, OrderType orderType, Integer quantity, Integer contractSize, OrderDirection orderDirection,
                          String accountNumber, boolean allOrNone, BigDecimal price){
        this.listingId = listingId;
        this.orderType = orderType;
        this.quantity = quantity;
        this.contractSize = contractSize;
        this.orderDirection = orderDirection;
        this.accountNumber = accountNumber;
        this.allOrNone = allOrNone;

        if(orderType == OrderType.LIMIT)
            this.limitPrice = price;
        if(orderType == OrderType.STOP)
            this.stopPrice = price;
    }
}
