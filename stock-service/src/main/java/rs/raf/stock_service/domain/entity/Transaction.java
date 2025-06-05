package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.IndexColumn;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @Column(precision = 20, scale = 6)
    private BigDecimal pricePerUnit;

    @Column(precision = 20, scale = 6)
    private BigDecimal totalPrice;

    private LocalDateTime timestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    public Transaction(Integer quantity, BigDecimal pricePerUnit, BigDecimal totalPrice, Order order) {
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalPrice = totalPrice;
        this.timestamp = LocalDateTime.now();
        this.order = order;
    }
}