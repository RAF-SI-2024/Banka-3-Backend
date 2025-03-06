package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.raf.bank_service.domain.enums.PaymentStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
@AllArgsConstructor
@Entity(name = "payments")
///  TRANSAKCIJA
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderName;

    @Column(nullable = false)
    private Long ClientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senderAccountNumber", referencedColumnName = "accountNumber", nullable = false)
    private Account senderAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    /// Za prenos izmedju dva racuna istog klijenta
    private String accountNumberReciver;
    /// Za placanje preko uplatnice
    @ManyToOne
    @JoinColumn(name = "payee_id")
    private Payee payee;

    private String paymentCode;

    private String purposeOfPayment;

    private String referenceNumber;

    private LocalDateTime transactionDate;

    private PaymentStatus paymentStatus;

    @PrePersist
    public void setTransactionDate() {
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }

}
