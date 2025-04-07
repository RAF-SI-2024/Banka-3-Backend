package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ListingDetailsDto;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.PriceHistoryDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;

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

    public ListingDto toDto(Listing listing, ListingPriceHistory dailyInfo) {
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

    public ListingDetailsDto toDetailsDto(Listing listing, List<ListingPriceHistory> priceHistory) {
        Integer contractSize = null;
        String contractUnit = null;

        // Ako je listing FuturesContract, postavi contractSize i contractUnit
        if (listing instanceof FuturesContract futures) {
            contractSize = futures.getContractSize();
            contractUnit = futures.getContractUnit();
        }

        // Mapiranje ListingPriceHistory u PriceHistoryDto sa novim poljima
        List<PriceHistoryDto> priceHistoryDtos = priceHistory.stream()
                .map(info -> new PriceHistoryDto(
                        info.getDate(),
                        info.getOpen(),
                        info.getHigh(),
                        info.getLow(),
                        info.getClose(),
                        info.getVolume()
                ))
                .collect(Collectors.toList());

        // Vraćanje prilagođenog ListingDetailsDto sa novim podacima
        return new ListingDetailsDto(
                listing.getId(),
                getListingType(listing),
                listing.getTicker(),
                listing.getName(),
                listing.getPrice(),
                listing.getExchange().getMic(),
                priceHistoryDtos,
                contractSize,
                contractUnit,
                null
        );
    }
}