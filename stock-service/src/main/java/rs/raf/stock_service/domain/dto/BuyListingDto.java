package rs.raf.stock_service.domain.dto;

import lombok.Data;
import rs.raf.stock_service.domain.enums.OrderType;

import javax.validation.constraints.NotNull;

@Data
public class BuyListingDto {

    @NotNull(message = "Listing id cannot be null")
    private Integer listingId;

    @NotNull(message = "Order type cannot be null")
    private OrderType orderType;

    private Integer quantity;

    @NotNull(message = "Contract size cannot be null")
    private Integer contractSize;
}
