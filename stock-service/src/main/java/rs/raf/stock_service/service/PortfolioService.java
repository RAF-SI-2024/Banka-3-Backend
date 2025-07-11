package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.BankClient;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
        import rs.raf.stock_service.domain.entity.*;
        import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.exceptions.InvalidListingTypeException;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.OptionNotEligibleException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.repository.*;
        import rs.raf.stock_service.domain.mapper.PortfolioMapper;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Collectors;

import rs.raf.stock_service.utils.JwtTokenUtil;

@Slf4j
@Service
@AllArgsConstructor
public class PortfolioService {

    private final PortfolioEntryRepository portfolioEntryRepository;
    private final UserClient userClient;
    private final ListingPriceHistoryRepository dailyPriceInfoRepository;
    private final OrderRepository orderRepository;
    private final BankClient bankClient;
    private final JwtTokenUtil jwtTokenUtil;

    public List<PortfolioEntryDto> getPortfolioForUser(Long userId) {
        return portfolioEntryRepository.findAllByUserId(userId).stream()
                .map(entry -> {
                    // Dohvatanje poslednjeg zapisa sa podacima o cenama
                    BigDecimal latestPrice = entry.getListing().getPrice();
                    BigDecimal profit = BigDecimal.ZERO;

                    if (latestPrice != null && entry.getAveragePrice() != null) {
                        // Profit sada koristi `close` umesto `price`
                        profit = latestPrice
                                .subtract(entry.getAveragePrice())
                                .multiply(BigDecimal.valueOf(entry.getAmount()));
                    }

                    // Mapiranje PortfolioEntry u PortfolioEntryDto
                    return PortfolioMapper.toDto(entry,
                            entry.getListing().getId(),
                            entry.getListing().getName(),
                            entry.getListing().getTicker(),
                            profit);
                }).collect(Collectors.toList());
    }

    public void setPublicAmount(Long userId, SetPublicAmountDto dto) {
        PortfolioEntry entry = portfolioEntryRepository.findByUserIdAndId(userId, dto.getPortfolioEntryId())
                .orElseThrow(PortfolioEntryNotFoundException::new);

        // samo stock moze
        if (entry.getType() != ListingType.STOCK) {
            throw new InvalidListingTypeException("Only STOCK type can be made public.");
        }

        if (dto.getPublicAmount() > entry.getAmount() - entry.getReservedAmount()) {
            throw new InvalidPublicAmountException("Public amount cannot exceed available amount.");
        }

        entry.setPublicAmount(dto.getPublicAmount());
        entry.setLastModified(LocalDateTime.now());

        portfolioEntryRepository.save(entry);
    }

    public List<PublicStockDto> getAllPublicStocks(Long userId, String role) {
        if (!role.equals("CLIENT")) {
            return Collections.emptyList();
        }

        List<PortfolioEntry> publicEntries = portfolioEntryRepository
                .findAllByTypeAndPublicAmountGreaterThan(ListingType.STOCK, 0);


        return publicEntries.stream().filter(entry -> !Objects.equals(entry.getUserId(), userId)).map(entry -> {
            Listing listing = entry.getListing();

            String ownerName;
            try {
                ClientDto client = userClient.getClientById(entry.getUserId());
                ownerName = client.getFirstName() + " " + client.getLastName();
            } catch (Exception e) {
                ActuaryDto actuary = userClient.getEmployeeById(entry.getUserId());
                ownerName = actuary.getFirstName() + " " + actuary.getLastName();
                return null; // skip actuary otc for now
            }

            BigDecimal currentPrice = listing.getPrice() != null ? listing.getPrice() : BigDecimal.ZERO;

            return PublicStockDto.builder()
                    .portfolioEntryId(entry.getId())
                    .security(ListingType.STOCK.name())
                    .ticker(listing.getTicker())
                    .amount(entry.getPublicAmount())
                    .price(currentPrice)
                    .lastModified(entry.getLastModified())
                    .owner(ownerName)
                    .build();

        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public TaxGetResponseDto getUserTaxes(Long userId) {

        List<Order> orders = orderRepository.findAllByUserId(userId);
        TaxGetResponseDto taxGetResponseDto = new TaxGetResponseDto();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minus(1, ChronoUnit.MONTHS);

        for (Order currOrder : orders) {
            if (currOrder.getTaxStatus() == null) continue;
            LocalDateTime currOrderDate = currOrder.getLastModification();
            if (currOrderDate.isAfter(oneMonthAgo) && currOrderDate.isBefore(now) && currOrder.getTaxStatus().equals(TaxStatus.PENDING)) {
                taxGetResponseDto.setUnpaidForThisMonth(taxGetResponseDto.getUnpaidForThisMonth().add(currOrder.getTaxAmount()));
            }
            if (currOrderDate.getYear() == now.getYear() && currOrder.getTaxStatus().equals(TaxStatus.PAID)) {
                taxGetResponseDto.setPaidForThisYear(taxGetResponseDto.getPaidForThisYear().add(currOrder.getTaxAmount()));
            }
        }

        taxGetResponseDto.setUnpaidForThisMonth(bankClient.convert(new ConvertDto("USD", "RSD", taxGetResponseDto.getUnpaidForThisMonth())));
        taxGetResponseDto.setPaidForThisYear(bankClient.convert(new ConvertDto("USD", "RSD", taxGetResponseDto.getPaidForThisYear())));

        return taxGetResponseDto;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void updateHoldingsOnOrderExecution(Transaction transaction) {
        Order order = transaction.getOrder();

        PortfolioEntry entry = portfolioEntryRepository
                .findByUserIdAndListing(order.getUserId(), order.getListing())
                .orElse(null);

        int totalQuantity = transaction.getQuantity();
        BigDecimal price = order.getPricePerUnit();

        if (order.getDirection() == OrderDirection.BUY) {
            if (entry == null) {
                entry = PortfolioEntry.builder()
                        .userId(order.getUserId())
                        .listing(order.getListing())
                        .type(order.getListing().getType())
                        .amount(totalQuantity)
                        .averagePrice(price)
                        .publicAmount(0)
                        .reservedAmount(0)
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

        } else if (entry != null) {
            int remaining = entry.getAmount() - totalQuantity;
            if (remaining <= 0) {
                portfolioEntryRepository.delete(entry);
            } else {
                entry.setAmount(remaining);
                entry.setReservedAmount(entry.getReservedAmount() - transaction.getQuantity());
                entry.setLastModified(LocalDateTime.now());
                portfolioEntryRepository.save(entry);
            }
        }
    }

    @Transactional
    public void updateHoldingsOnOtcOptionExecution(Long fromUserId, Long toUserId, Stock stock, int quantity, BigDecimal pricePerStock) {
        PortfolioEntry sellerEntry = portfolioEntryRepository.findByUserIdAndListing(fromUserId, stock)
                .orElseThrow(PortfolioEntryNotFoundException::new); //ne sme da se desi ovaj exception

        sellerEntry.setAmount(sellerEntry.getAmount() - quantity);
        sellerEntry.setReservedAmount(sellerEntry.getReservedAmount() - quantity);
        sellerEntry.setLastModified(LocalDateTime.now());

        if (sellerEntry.getAmount() == 0) {
            portfolioEntryRepository.delete(sellerEntry);
        } else {
            portfolioEntryRepository.save(sellerEntry);
        }

        PortfolioEntry buyerEntry = portfolioEntryRepository.findByUserIdAndListing(toUserId, stock).orElse(null);

        if (buyerEntry == null) {
            buyerEntry = PortfolioEntry.builder()
                    .userId(toUserId)
                    .listing(stock)
                    .type(ListingType.STOCK)
                    .amount(quantity)
                    .averagePrice(pricePerStock)
                    .publicAmount(0)
                    .reservedAmount(0)
                    .inTheMoney(false)
                    .used(false)
                    .lastModified(LocalDateTime.now())
                    .build();
        } else {
            int currentAmount = buyerEntry.getAmount();
            BigDecimal currentAvgPrice = buyerEntry.getAveragePrice();

            BigDecimal totalCost = currentAvgPrice.multiply(BigDecimal.valueOf(currentAmount))
                    .add(pricePerStock.multiply(BigDecimal.valueOf(quantity)));
            int newTotalAmount = currentAmount + quantity;
            BigDecimal newAvgPrice = totalCost.divide(BigDecimal.valueOf(newTotalAmount), RoundingMode.HALF_UP);

            buyerEntry.setAmount(newTotalAmount);
            buyerEntry.setAveragePrice(newAvgPrice);
            buyerEntry.setLastModified(LocalDateTime.now());
        }

        portfolioEntryRepository.save(buyerEntry);
    }
}
