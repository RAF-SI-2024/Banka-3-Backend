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
    private Long userId;

    @Column(nullable = false)
    private String userRole;

    @ManyToOne
    @JoinColumn(name = "listing_id", nullable = false, updatable = false)
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrderType orderType; // market, limit, stop, stop_limit

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private OrderDirection direction; // buy, sell

    @Column(nullable = false, updatable = false)
    private boolean allOrNone;

    @Column(nullable = false, updatable = false)
    private Integer contractSize;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal pricePerUnit;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private String accountNumber;

    @Column
    private BigDecimal commission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // pending, approved, declined, partial, done, cancelled

    private Long approvedBy;

    @Column(nullable = false)
    private Boolean isDone;

    @Column(nullable = false)
    private LocalDateTime lastModification;

    @Column(updatable = false)
    private Boolean afterHours;

    private BigDecimal stopPrice;
    private boolean stopFulfilled;

    @Column(nullable = false)
    private Integer remainingPortions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Transaction> transactions;

    @Enumerated(EnumType.STRING)
    private TaxStatus taxStatus;
    private BigDecimal taxAmount;
    private BigDecimal profit;

    public Order(Long userId, String userRole, Listing listing, OrderType orderType, OrderDirection direction, boolean allOrNone,
                 Integer contractSize, Integer quantity, BigDecimal pricePerUnit, String accountNumber, BigDecimal stopPrice,
                 boolean afterHours) {
        this.userId = userId;
        this.userRole = userRole;
        this.listing = listing;
        this.orderType = orderType;
        this.direction = direction;
        this.allOrNone = allOrNone;
        this.contractSize = contractSize;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.accountNumber = accountNumber;
        this.stopPrice = stopPrice;
        this.afterHours = afterHours;

        remainingPortions = quantity;
        totalPrice = BigDecimal.ZERO;
        stopFulfilled = false;
        status = OrderStatus.PENDING;
        isDone = false;
        lastModification = LocalDateTime.now();
        transactions = new ArrayList<>();
    }
}

