package rs.raf.stock_service.domain.entity;

import lombok.*;
import org.hibernate.annotations.BatchSize;
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
    private OrderStatus status; // pending, approved, declined, done, cancelled

    private Long approvedBy;

    @Column(nullable = false)
    private Boolean isDone;

    @Column(nullable = false)
    private LocalDateTime lastModification;

    @Column(nullable = false)
    private Integer remainingPortions;

    @Column(updatable = false)
    private Boolean afterHours;

    @Column(nullable = false)
    private String accountNumber;

    @Column(updatable = false)
    private BigDecimal stopPrice;

    private boolean stopFulfilled;

    @Column(nullable = false, updatable = false)
    private boolean allOrNone;

    private BigDecimal reservedAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Transaction> transactions;

    @Enumerated(EnumType.STRING)
    private TaxStatus taxStatus;
    private BigDecimal taxAmount;
    private BigDecimal profit;

    private String role;

    public Order(Long userId, Listing listing, OrderType orderType, Integer quantity, Integer contractSize, BigDecimal pricePerUnit,
                 OrderDirection direction, boolean afterHours, String accountNumber, BigDecimal stopPrice, boolean allOrNone,
                 String role) {
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
        this.allOrNone = allOrNone;
        this.status = OrderStatus.PENDING;
        this.isDone = false;
        this.lastModification = LocalDateTime.now();
        this.stopFulfilled = false;
        this.transactions = new ArrayList<>();
        this.role = role;
    }
}

