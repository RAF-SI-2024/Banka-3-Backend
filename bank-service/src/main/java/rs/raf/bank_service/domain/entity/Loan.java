package rs.raf.bank_service.domain.entity;

import lombok.*;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loanNumber;

    @Enumerated(EnumType.STRING)
    private LoanType type;

    private BigDecimal amount;

    private Integer repaymentPeriod;

    private BigDecimal nominalInterestRate;

    private BigDecimal effectiveInterestRate;

    // Dodato za podršku varijabilne kamate
    private BigDecimal baseInterestRate;

    private BigDecimal rateDelta;

    private BigDecimal bankMargin;

    private LocalDate startDate;

    private LocalDate dueDate;

    private BigDecimal nextInstallmentAmount;

    private LocalDate nextInstallmentDate;

    private BigDecimal remainingDebt;

    @ManyToOne
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @Enumerated(EnumType.STRING)
    private InterestRateType interestRateType;

    @ManyToOne
    private Account account;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    private List<Installment> installments;
}