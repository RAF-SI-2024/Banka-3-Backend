package rs.raf.stock_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String user; // aktuar

    @Column(nullable = false)
    private Integer asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType; // market, limit, stop, stop_limit

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer contractSize;

    @Column(nullable = false)
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderDirection direction; // buy, sell

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // pending, approved, declined

    private String approvedBy;

    @Column(nullable = false)
    private Boolean isDone;

    @Column(nullable = false)
    private LocalDateTime lastModification;

    @Column(nullable = false)
    private Integer remainingPortions;

    @Column(nullable = false)
    private Boolean afterHours;


}

