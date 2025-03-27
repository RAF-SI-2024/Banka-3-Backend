package rs.raf.stock_service.domain.mapper;

import rs.raf.stock_service.domain.dto.CreateOrderDto;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.OrderDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.Order;

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

    public static Order toOrder(CreateOrderDto createOrderDto, Long userId, Listing listing, boolean afterHours){
        return new Order(
                userId,
                listing,
                createOrderDto.getOrderType(),
                createOrderDto.getQuantity(),
                createOrderDto.getContractSize(),
                createOrderDto.getOrderDirection(),
                afterHours,
                createOrderDto.getAccountNumber()
        );
    }
}
