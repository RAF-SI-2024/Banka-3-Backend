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
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public static OrderDto toDto(Order order, ListingDto listingDto, String clientName, String accountNumber) {
        return new OrderDto(
                order.getId(),
                order.getUserId(),
                listingDto,
                order.getOrderType(),
                order.getQuantity(),
                order.getContractSize(),
                order.getPricePerUnit(),
                order.getDirection(),
                order.getStatus(),
                order.getApprovedBy(),
                order.getIsDone(),
                order.getLastModification(),
                order.getRemainingPortions(),
                order.getStopPrice(),
                order.isStopFulfilled(),
                order.getAfterHours(),
                order.getTransactions() == null ? null :
                        order.getTransactions().stream().map(TransactionMapper::toDto).collect(Collectors.toList()),
                order.getProfit(),
                clientName,
                accountNumber
        );
    }

    public static Order toOrder(CreateOrderDto createOrderDto, Long userId, Listing listing, String role) {
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
                listing,
                createOrderDto.getOrderType(),
                createOrderDto.getQuantity(),
                createOrderDto.getContractSize(),
                pricePerUnit,
                createOrderDto.getOrderDirection(),
                afterHours(listing.getExchange()),
                createOrderDto.getAccountNumber(),
                stopPrice,
                createOrderDto.isAllOrNone(),
                role
        );
    }

    private static boolean afterHours(Exchange exchange){
        LocalTime closeTime = exchange.getPolity().getCloseTime();
        LocalTime afterHoursTime = closeTime.plusHours(4);
        LocalTime nowInTimeZone = LocalTime.now().plusHours(exchange.getTimeZone() -
                (OffsetDateTime.now().getOffset().getTotalSeconds() / 3600));

        if (closeTime.isBefore(afterHoursTime))
            return nowInTimeZone.isAfter(closeTime) && nowInTimeZone.isBefore(afterHoursTime);
        else
            return nowInTimeZone.isAfter(closeTime) || nowInTimeZone.isBefore(afterHoursTime);
    }
}
