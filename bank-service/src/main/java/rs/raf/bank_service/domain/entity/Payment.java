package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
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
    private Long clientId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "senderAccountNumber", referencedColumnName = "accountNumber", nullable = false)
    private Account senderAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private BigDecimal amount;

    /// Za prenos izmedju dva racuna istog klijenta
    private String accountNumberReceiver;
    /// Za placanje preko uplatnice
    @ManyToOne
    @JoinColumn(name = "payee_id")
    private Payee payee;

    private String paymentCode;

    private String purposeOfPayment;

    private String referenceNumber;

    @CreationTimestamp
    private LocalDateTime date;

    private BigDecimal outAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column()
    private Long receiverClientId;

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", senderName='" + senderName + '\'' +
                ", clientId=" + clientId +
                ", senderAccount=" + senderAccount +
                ", card=" + card +
                ", amount=" + amount +
                ", accountNumberReceiver='" + accountNumberReceiver + '\'' +
                ", payee=" + payee +
                ", paymentCode='" + paymentCode + '\'' +
                ", purposeOfPayment='" + purposeOfPayment + '\'' +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", date=" + date +
                ", outAmount=" + outAmount +
                ", status=" + status +
                ", receiverClientId=" + receiverClientId +
                '}';
    }
}
