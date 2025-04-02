package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.dto.TaxGetResponseDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.domain.mapper.PortfolioMapper;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PortfolioService {

    private final PortfolioEntryRepository portfolioEntryRepository;
    private final ListingPriceHistoryRepository dailyPriceInfoRepository;
    private final ListingDailyPriceInfoRepository dailyPriceInfoRepository;
    private final OrderRepository orderRepository;

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
                    // Dohvatanje poslednjeg zapisa sa podacima o cenama
                    var latestPrice = entry.getListing().getPrice();
                    BigDecimal profit = BigDecimal.ZERO;

                    if (latestPrice != null ) {
                        // Profit sada koristi `close` umesto `price`
                        profit = latestPrice
                                .subtract(entry.getAveragePrice())
                                .multiply(BigDecimal.valueOf(entry.getAmount()));
                    }

                    // Mapiranje PortfolioEntry u PortfolioEntryDto
                    return PortfolioMapper.toDto(entry,
                            entry.getListing().getName(),
                            entry.getListing().getTicker(),
                            profit);
                }).collect(Collectors.toList());
    }
    public TaxGetResponseDto getTaxes(Long userId){
        
        List<Order> orders = orderRepository.findAllByUserId(userId);
        TaxGetResponseDto taxGetResponseDto = new TaxGetResponseDto();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minus(1, ChronoUnit.MONTHS);

        for(Order currOrder : orders){
            LocalDateTime currOrderDate = currOrder.getLastModification();
            if(currOrderDate.isAfter(oneMonthAgo) && currOrderDate.isBefore(now) && currOrder.getTaxStatus().equals(TaxStatus.PENDING)){
                taxGetResponseDto.setUnpaidForThisMonth(taxGetResponseDto.getUnpaidForThisMonth().add(currOrder.getTaxAmount()));
            }
            if(currOrderDate.getYear() == now.getYear() && currOrder.getTaxStatus().equals(TaxStatus.PAID)){
                taxGetResponseDto.setPaidForThisYear(taxGetResponseDto.getPaidForThisYear().add(currOrder.getTaxAmount()));
            }
        }
        return taxGetResponseDto;
    }
}
