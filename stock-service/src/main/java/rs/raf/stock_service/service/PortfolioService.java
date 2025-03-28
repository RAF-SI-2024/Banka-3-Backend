package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.domain.mapper.PortfolioMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PortfolioService {

    private final PortfolioEntryRepository portfolioEntryRepository;
    private final ListingDailyPriceInfoRepository dailyPriceInfoRepository;

    public void updateHoldingsOnOrderExecution(Order order) {
        if (!order.getIsDone()) return;

        PortfolioEntry entry = portfolioEntryRepository
                .findByUserIdAndListing(order.getUserId(), order.getListing())
                .orElse(null);

        int totalQuantity = order.getQuantity() * order.getContractSize();
        BigDecimal price = order.getPricePerUnit();

        if (order.getDirection() == OrderDirection.BUY) {
            if (entry == null) {
                entry = PortfolioEntry.builder()
                        .userId(order.getUserId())
                        .listing(order.getListing())
                        .type(ListingType.valueOf(order.getListing().getClass().getSimpleName().toUpperCase())) // npr. "STOCK"
                        .amount(totalQuantity)
                        .averagePrice(price)
                        .isPublic(false)
                        .inTheMoney(false)
                        .used(false)
                        .lastModified(LocalDateTime.now())
                        .build();
            } else {
                int newAmount = entry.getAmount() + totalQuantity;
                BigDecimal oldTotal = entry.getAveragePrice().multiply(BigDecimal.valueOf(entry.getAmount()));
                BigDecimal newTotal = price.multiply(BigDecimal.valueOf(totalQuantity));
                BigDecimal avgPrice = oldTotal.add(newTotal).divide(BigDecimal.valueOf(newAmount), RoundingMode.HALF_UP);

                entry.setAmount(newAmount);
                entry.setAveragePrice(avgPrice);
                entry.setLastModified(LocalDateTime.now());
            }
            portfolioEntryRepository.save(entry);

        } else if (order.getDirection() == OrderDirection.SELL && entry != null) {
            int remaining = entry.getAmount() - totalQuantity;
            if (remaining <= 0) {
                portfolioEntryRepository.delete(entry);
            } else {
                entry.setAmount(remaining);
                entry.setLastModified(LocalDateTime.now());
                portfolioEntryRepository.save(entry);
            }
        }
    }

    public List<PortfolioEntryDto> getPortfolioForUser(Long userId) {
        return portfolioEntryRepository.findAllByUserId(userId).stream()
                .map(entry -> {
                    var latestPrice = dailyPriceInfoRepository.findTopByListingOrderByDateDesc(entry.getListing());
                    BigDecimal profit = BigDecimal.ZERO;

                    if (latestPrice != null) {
                        profit = latestPrice.getPrice()
                                .subtract(entry.getAveragePrice())
                                .multiply(BigDecimal.valueOf(entry.getAmount()));
                    }

                    return PortfolioMapper.toDto(entry,
                            entry.getListing().getName(),
                            entry.getListing().getTicker(),
                            profit);
                }).collect(Collectors.toList());
    }
}
