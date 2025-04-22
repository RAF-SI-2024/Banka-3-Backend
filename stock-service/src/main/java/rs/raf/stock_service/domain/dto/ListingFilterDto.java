package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ListingFilterDto {
    private String type;
    private String search;
    private String exchangePrefix;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minAsk;
    private BigDecimal maxAsk;
    private BigDecimal minBid;
    private BigDecimal maxBid;
    private Long minVolume;
    private Long maxVolume;
    private BigDecimal minMaintenanceMargin;
    private BigDecimal maxMaintenanceMargin;
    private LocalDate settlementDate;
    private String sortBy;
    private String sortOrder;

    @Override
    public String toString() {
        return "ListingFilterDto{" +
                "type='" + type + '\'' +
                ", search='" + search + '\'' +
                ", exchangePrefix='" + exchangePrefix + '\'' +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", minAsk=" + minAsk +
                ", maxAsk=" + maxAsk +
                ", minBid=" + minBid +
                ", maxBid=" + maxBid +
                ", minVolume=" + minVolume +
                ", maxVolume=" + maxVolume +
                ", minMaintenanceMargin=" + minMaintenanceMargin +
                ", maxMaintenanceMargin=" + maxMaintenanceMargin +
                ", settlementDate=" + settlementDate +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}
