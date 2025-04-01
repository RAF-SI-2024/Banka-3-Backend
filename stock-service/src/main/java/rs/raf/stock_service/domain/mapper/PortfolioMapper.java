package rs.raf.stock_service.domain.mapper;

import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
import rs.raf.stock_service.domain.entity.PortfolioEntry;

public class PortfolioMapper {

    public static PortfolioEntryDto toDto(PortfolioEntry entry, String listingName, String ticker, java.math.BigDecimal profit) {
        return PortfolioEntryDto.builder()
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
                .build();
    }
}
