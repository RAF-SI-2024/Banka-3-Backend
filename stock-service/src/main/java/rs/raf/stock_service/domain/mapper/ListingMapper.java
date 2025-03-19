package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ListingDetailsDto;
import rs.raf.stock_service.domain.dto.PriceHistoryDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.enums.ListingType;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListingMapper {

    public ListingType getListingType(Listing listing) {
        if (listing instanceof Stock) {
            return ListingType.STOCK;
        } else if (listing instanceof FuturesContract) {
            return ListingType.FUTURES;
        } else if (listing instanceof ForexPair) {
            return ListingType.FOREX;
        } else if (listing instanceof Option) {
            return ListingType.OPTION;
        }
        return null;
    }

    public ListingDto toDto(Listing listing, ListingDailyPriceInfo dailyInfo) {
        return new ListingDto(
                listing.getId(),
                getListingType(listing),
                listing.getTicker(),
                listing.getPrice(),
                dailyInfo != null ? dailyInfo.getChange() : null,
                dailyInfo != null ? dailyInfo.getVolume() : null,
                listing.getPrice().multiply(new java.math.BigDecimal("1.1")),
                listing.getExchange() != null ? listing.getExchange().getMic() : null,
                listing.getAsk()
        );
    }

    public ListingDetailsDto toDetailsDto(Listing listing, List<ListingDailyPriceInfo> priceHistory) {
        Integer contractSize = null;
        String contractUnit = null;

        if (listing instanceof FuturesContract futures) {
            contractSize = futures.getContractSize();
            contractUnit = futures.getContractUnit();
        }

        return new ListingDetailsDto(
                listing.getId(),
                getListingType(listing),
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