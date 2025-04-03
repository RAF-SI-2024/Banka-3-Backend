package rs.raf.stock_service.domain.dto;


import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import rs.raf.stock_service.domain.enums.ListingType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Builder
public class PortfolioEntryDto {

    private Long id;

    private String securityName;     // listing.name
    private String ticker;           // listing.ticker
    private ListingType type;        // npr. STOCK, OPTION...
    private Integer amount;
    private BigDecimal averagePrice;
    private BigDecimal profit;
    private LocalDateTime lastModified;

    private Integer publicAmount;
    private Boolean inTheMoney;      // samo za opcije
    private Boolean used;            // samo za opcije




}
