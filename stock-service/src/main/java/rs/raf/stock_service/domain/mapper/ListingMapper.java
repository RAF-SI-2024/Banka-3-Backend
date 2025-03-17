package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingDailyPriceInfo;
import rs.raf.stock_service.domain.dto.ListingDto;
import java.math.BigDecimal;

@Component
public class ListingMapper {

    public ListingDto toDto(Listing listing, ListingDailyPriceInfo dailyInfo) {
        BigDecimal maintenanceMargin = listing.getPrice() != null ? listing.getPrice().multiply(BigDecimal.valueOf(0.1)) : BigDecimal.ZERO;
        BigDecimal initialMarginCost = maintenanceMargin.multiply(BigDecimal.valueOf(1.1));

        return new ListingDto(
                listing.getId(),
                listing.getTicker(),
                listing.getPrice(),
                dailyInfo != null ? dailyInfo.getChange() : BigDecimal.ZERO,
                dailyInfo != null ? dailyInfo.getVolume() : 0L,
                initialMarginCost
        );
    }
}