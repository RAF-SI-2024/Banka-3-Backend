package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ListingDetailsDto;
import rs.raf.stock_service.domain.dto.PriceHistoryDto;
import rs.raf.stock_service.domain.entity.FuturesContract;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingDailyPriceInfo;
import rs.raf.stock_service.domain.dto.ListingDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListingMapper {

    public ListingDto toDto(Listing listing, ListingDailyPriceInfo dailyInfo) {
        return new ListingDto(
                listing.getId(),
                listing.getTicker(),
                listing.getPrice(),
                dailyInfo != null ? dailyInfo.getChange() : null,
                dailyInfo != null ? dailyInfo.getVolume() : null,
                listing.getPrice().multiply(new java.math.BigDecimal("1.1")),
                listing.getExchange() != null ? listing.getExchange().getMic() : null
        );
    }

    public ListingDetailsDto toDetailsDto(Listing listing, List<ListingDailyPriceInfo> priceHistory) {
        Integer contractSize = null;
        String contractUnit = null;

        if (listing instanceof FuturesContract) {
            FuturesContract futures = (FuturesContract) listing;
            contractSize = futures.getContractSize();
            contractUnit = futures.getContractUnit();
        }

        return new ListingDetailsDto(
                listing.getId(),
                listing.getTicker(),
                listing.getName(),
                listing.getPrice(),
                listing.getExchange().getMic(),
                priceHistory.stream()
                        .map(info -> new PriceHistoryDto(info.getDate(), info.getPrice()))
                        .collect(Collectors.toList()),
                contractSize,
                contractUnit
        );
    }
}