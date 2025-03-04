package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreditDetailedDTO {
    private Long id;
    private String accountNumber;
    private String creditType;
    private BigDecimal amount;
    private int repaymentPeriodMonths;
    private BigDecimal interestRate;
    private LocalDate contractDate;
    private LocalDate dueDate;
    private BigDecimal installmentAmount;
    private LocalDate nextInstallmentDate;
    private BigDecimal remainingBalance;
    private Currency currency;

    public CreditDetailedDTO(Long id, String accountNumber, String creditType, BigDecimal amount, int repaymentPeriodMonths, BigDecimal interestRate,
                             LocalDate contractDate, LocalDate dueDate, BigDecimal installmentAmount, LocalDate nextInstallmentDate,
                             BigDecimal remainingBalance, Currency currency) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.creditType = creditType;
        this.amount = amount;
        this.repaymentPeriodMonths = repaymentPeriodMonths;
        this.interestRate = interestRate;
        this.contractDate = contractDate;
        this.dueDate = dueDate;
        this.installmentAmount = installmentAmount;
        this.nextInstallmentDate = nextInstallmentDate;
        this.remainingBalance = remainingBalance;
        this.currency = currency;
    }
}
