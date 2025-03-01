package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Credit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private BigDecimal amount;

    @ManyToOne
    private Currency currency;

    private int repaymentPeriodMonths;
    private BigDecimal interestRate;
    private LocalDate contractDate;
    private LocalDate dueDate;
    private BigDecimal installmentAmount;
    private LocalDate nextInstallmentDate;
    private BigDecimal remainingBalance;
}