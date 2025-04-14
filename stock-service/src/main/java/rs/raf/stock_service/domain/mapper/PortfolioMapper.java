package rs.raf.stock_service.domain.mapper;

import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
import rs.raf.stock_service.domain.entity.PortfolioEntry;

public class PortfolioMapper {

    public static PortfolioEntryDto toDto(PortfolioEntry entry, Long listingId, String listingName, String ticker, java.math.BigDecimal profit) {
        return PortfolioEntryDto.builder()
                .id(entry.getId())
                .listingId(listingId)
                .securityName(listingName)
                .ticker(ticker)
                .type(entry.getType())
                .amount(entry.getAmount())
                .averagePrice(entry.getAveragePrice())
                .profit(profit)
                .lastModified(entry.getLastModified())
                .publicAmount(entry.getPublicAmount())
                .inTheMoney(entry.getInTheMoney())
                .used(entry.getUsed())
                .currentPrice(entry.getListing() != null ? entry.getListing().getPrice() : null) // dodatak ovde
                .build();
    }
}
