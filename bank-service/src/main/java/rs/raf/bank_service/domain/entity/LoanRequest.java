package rs.raf.bank_service.domain.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import rs.raf.bank_service.domain.enums.EmploymentStatus;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LoanType type;

    private BigDecimal amount;
    private String purpose;
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    private EmploymentStatus employmentStatus;

    private Integer employmentDuration;
    private Integer repaymentPeriod;
    private String contactPhone;
    @ManyToOne
    @JoinColumn(name = "account_number")
    private Account account;
    @ManyToOne
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private LoanRequestStatus status;
    @Enumerated(EnumType.STRING)
    private InterestRateType interestRateType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}