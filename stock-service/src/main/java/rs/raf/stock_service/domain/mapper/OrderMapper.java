package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.CreateOrderDto;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderDirection;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public static OrderDto toDto(Order order, ListingDto listingDto, String userName, String accountNumber) {
        return new OrderDto(
                order.getId(),
                order.getUserId(),
                userName,
                listingDto,
                accountNumber,
                order.getOrderType(),
                order.getDirection(),
                order.getContractSize(),
                order.getQuantity(),
                order.getPricePerUnit(),
                order.getTotalPrice(),
                order.getCommission(),
                order.getStatus(),
                order.getApprovedBy(),
                order.getIsDone(),
                order.getLastModification(),
                order.getRemainingPortions(),
                order.getStopPrice(),
                order.isStopFulfilled(),
                order.getAfterHours(),
                order.getProfit(),
                order.getTransactions() == null ? null :
                        order.getTransactions().stream().map(TransactionMapper::toDto).collect(Collectors.toList())
        );
    }

    public static Order toOrder(CreateOrderDto createOrderDto, Long userId, String role, Listing listing) {
        BigDecimal pricePerUnit = null;
        BigDecimal stopPrice = null;

        //valjda je price ustvari BID price, jer promenljiva bid ne postoji
        switch(createOrderDto.getOrderType()){
            case MARKET:
                if (createOrderDto.getOrderDirection() == OrderDirection.BUY)
                    pricePerUnit =  listing.getAsk() == null ? listing.getPrice() : listing.getAsk();
                else
                    pricePerUnit = listing.getPrice();
                break;
            case LIMIT:
                pricePerUnit = createOrderDto.getLimitPrice();
                break;
            case STOP:
                pricePerUnit = createOrderDto.getStopPrice();
                stopPrice = createOrderDto.getStopPrice();
                break;
            case STOP_LIMIT:
                pricePerUnit = createOrderDto.getLimitPrice();
                stopPrice = createOrderDto.getStopPrice();
        }

        return new Order(
                userId,
                role,
                listing,
                createOrderDto.getOrderType(),
                createOrderDto.getOrderDirection(),
                createOrderDto.isAllOrNone(),
                createOrderDto.getContractSize(),
                createOrderDto.getQuantity(),
                pricePerUnit,
                createOrderDto.getAccountNumber(),
                stopPrice,
                afterHours(listing.getExchange())
        );
    }

    private static boolean afterHours(Exchange exchange) {
        if (exchange.isTestMode()) return false;

        // Pretpostavka: exchange.getTimeZone() vraća npr. "America/New_York" ili "Europe/Belgrade"
        ZoneId zoneId = ZoneId.of(String.valueOf(exchange.getTimeZone()));
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // Dobijanje vremena zatvaranja berze (npr. 17:00)
        LocalTime closeTime = exchange.getPolity().getCloseTime();
        LocalTime openTime = exchange.getPolity().getOpenTime();

        // Ako berza radi 24/7, nema after hours
        if (openTime.equals(LocalTime.MIDNIGHT) && closeTime.equals(LocalTime.of(23, 59))) {
            return false;
        }

        // Konstruisanje današnjeg zatvaranja
        ZonedDateTime closeDateTime = now.with(closeTime);
        if (now.toLocalTime().isBefore(closeTime)) {
            // Ako je trenutno vreme pre vremena zatvaranja, znači zatvaranje je bilo juče
            closeDateTime = closeDateTime.minusDays(1);
        }

        // After hours traje 4 sata nakon zatvaranja
        ZonedDateTime afterHoursEnd = closeDateTime.plusHours(4);

        // Provera da li je sadašnje vreme između zatvaranja i kraja after hours
        return !now.isBefore(closeDateTime) && now.isBefore(afterHoursEnd);
    }
}
