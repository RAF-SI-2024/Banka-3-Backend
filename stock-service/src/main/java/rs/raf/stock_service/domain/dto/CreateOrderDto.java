package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderType;

import javax.validation.constraints.NotNull;

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
    private String  accountNumber;
}
