package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OrderDto {

    private Long id;
    private Long userId;
    private Integer asset;
    private OrderType orderType;
    private Integer quantity;
    private Integer contractSize;
    private BigDecimal pricePerUnit;
    private OrderDirection direction;
    private OrderStatus status;
    private Long approvedBy;
    private Boolean isDone;
    private LocalDateTime lastModification;
    private Integer remainingPortions;
    private Boolean afterHours;

    public OrderDto(Order order) {
        this.id = order.getId();
        this.userId = order.getUserId();
        this.asset = order.getAsset();
        this.orderType = order.getOrderType();
        this.quantity = order.getQuantity();
        this.contractSize = order.getContractSize();
        this.pricePerUnit = order.getPricePerUnit();
        this.direction = order.getDirection();
        this.status = order.getStatus();
        this.approvedBy = order.getApprovedBy();
        this.isDone = order.getIsDone();
        this.lastModification = order.getLastModification();
        this.remainingPortions = order.getRemainingPortions();
        this.afterHours = order.getAfterHours();
    }
}
