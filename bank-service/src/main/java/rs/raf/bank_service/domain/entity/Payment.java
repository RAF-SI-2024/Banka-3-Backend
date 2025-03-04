package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
@AllArgsConstructor
@Entity(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senderAccountNumber", referencedColumnName = "accountNumber", nullable = false)
    private Account senderAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiverAccountNumber", referencedColumnName = "accountNumber", nullable = false)
    private Account receiverAccount;

    @Column(nullable = false)
    private String paymentCode;

    @Column(nullable = false)
    private String purposeOfPayment;

    @Column(nullable = true)
    private String referenceNumber;

    private LocalDateTime transactionDate;


    @PrePersist
    public void setTransactionDate() {
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }

}
