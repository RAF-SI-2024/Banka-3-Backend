package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
import rs.raf.stock_service.domain.dto.PublicStockDto;
import rs.raf.stock_service.domain.dto.SetPublicAmountDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.exceptions.InvalidListingTypeException;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
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
                        .type(order.getListing().getType())
                        .amount(totalQuantity)
                        .averagePrice(price)
                        .publicAmount(0) // privremeno 0 moze neka logika kasnije kad bude bilo potrebno
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

    public void setPublicAmount(Long userId, SetPublicAmountDto dto) {
        PortfolioEntry entry = portfolioEntryRepository.findByUserIdAndListingId(userId, dto.getListingId())
                .orElseThrow(() -> new PortfolioEntryNotFoundException("Portfolio entry not found"));

        // samo stock moze
        if (entry.getType() != ListingType.STOCK) {
            throw new InvalidListingTypeException("Only STOCK type can be made public.");
        }


        if (dto.getPublicAmount() > entry.getAmount()) {
            throw new InvalidPublicAmountException("Public amount cannot exceed owned amount.");
        }

        entry.setPublicAmount(dto.getPublicAmount());
        entry.setLastModified(LocalDateTime.now());

        portfolioEntryRepository.save(entry);
    }


    public List<PublicStockDto> getAllPublicStocks() {
        List<PortfolioEntry> publicEntries = portfolioEntryRepository.findAllByTypeAndPublicAmountGreaterThan(ListingType.STOCK, 0);

        return publicEntries.stream().map(entry -> {
            Listing listing = entry.getListing();


            ListingDailyPriceInfo latestPriceInfo = dailyPriceInfoRepository
                    .findTopByListingOrderByDateDesc(listing);

            BigDecimal currentPrice = latestPriceInfo != null ? latestPriceInfo.getPrice() : BigDecimal.ZERO;
            BigDecimal avgPrice = entry.getAveragePrice() != null ? entry.getAveragePrice() : BigDecimal.ZERO;

            BigDecimal profit = currentPrice.subtract(avgPrice)
                    .multiply(BigDecimal.valueOf(entry.getPublicAmount()));

            return PublicStockDto.builder()
                    .security(ListingType.STOCK.name())
                    .ticker(listing.getTicker())
                    .amount(entry.getPublicAmount())
                    .price(currentPrice)
                    .profit(profit)
                    .lastModified(entry.getLastModified())
                    .owner("user-" + entry.getUserId())               // privremeni owner
                    .build();

        }).collect(Collectors.toList());
    }
}
