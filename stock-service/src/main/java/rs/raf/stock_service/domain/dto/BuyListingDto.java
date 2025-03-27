package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.stock_service.domain.enums.OrderType;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuyListingDto {

    @NotNull(message = "Listing id cannot be null")
    private Integer listingId;

    @NotNull(message = "Order type cannot be null")
    private OrderType orderType;

    private Integer quantity;

    @NotNull(message = "Contract size cannot be null")
    private Integer contractSize;

    @NotNull(message = "Account number cannot be null")
    private String accountNumber;
}
