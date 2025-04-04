package rs.raf.stock_service.domain.mapper;

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

public class OrderMapper {

    public static OrderDto toDto(Order order, ListingDto listingDto) {
        if (order == null) return null;
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
                order.getAfterHours(),
                order.getTransactions().stream().map(TransactionMapper::toDto).collect(Collectors.toList())
        );
    }

    public static Order toOrder(CreateOrderDto createOrderDto, Long userId, Listing listing) {
        BigDecimal pricePerUnit = null;
        BigDecimal stopPrice = null;

        //valjda je price ustvari BID price, jer promenljiva bid ne postoji
        switch(createOrderDto.getOrderType()){
            case MARKET:
                if (createOrderDto.getOrderDirection() == OrderDirection.BUY)
                    pricePerUnit =  listing.getAsk() == null ? BigDecimal.ONE : listing.getAsk();
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
                stopPrice
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
