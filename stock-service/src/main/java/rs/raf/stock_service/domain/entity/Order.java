package rs.raf.stock_service.domain.entity;

import lombok.*;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.domain.enums.TaxStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId; // aktuar

    @ManyToOne
    @JoinColumn(name = "listing_id", nullable = false, updatable = false)
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrderType orderType; // market, limit, stop, stop_limit

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(nullable = false, updatable = false)
    private Integer contractSize;

    @Column(nullable = false)
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrderDirection direction; // buy, sell

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // pending, approved, declined, done

    private Long approvedBy;

    @Column(nullable = false)
    private Boolean isDone;

    @Column(nullable = false)
    private LocalDateTime lastModification;

    private Integer remainingPortions;

    @Column(updatable = false)
    private Boolean afterHours;

    private String accountNumber;
    private BigDecimal stopPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @Enumerated(EnumType.STRING)
    private TaxStatus taxStatus;
    private BigDecimal taxAmount;


    public Order(Long userId, Listing listing, OrderType orderType, Integer quantity, Integer contractSize, BigDecimal pricePerUnit,
                 OrderDirection direction, boolean afterHours, String accountNumber, BigDecimal stopPrice) {
        this.userId = userId;
        this.listing = listing;
        this.orderType = orderType;
        this.quantity = quantity;
        this.contractSize = contractSize;
        this.direction = direction;
        this.afterHours = afterHours;
        this.accountNumber = accountNumber;
        this.remainingPortions = quantity;
        this.pricePerUnit = pricePerUnit;
        this.stopPrice = stopPrice;
        this.status = OrderStatus.PENDING;
        this.isDone = false;
        this.lastModification = LocalDateTime.now();
        this.transactions = new ArrayList<>();
    }
}

