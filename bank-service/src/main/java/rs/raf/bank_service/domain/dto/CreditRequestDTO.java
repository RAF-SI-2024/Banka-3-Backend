package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;

import java.math.BigDecimal;

@Getter
@Setter

public class CreditRequestDTO {
    private Long id;

    private String accountNumber;
    private String creditType;
    private BigDecimal amount;
    private Currency currency;
    private String purpose;
    private BigDecimal monthlySalary;
    private String employmentStatus;
    private int employmentPeriod;
    private int repaymentPeriod;
    private String branch;
    private String phoneNumber;
    private CreditRequestApproval approval;
    private Long bankAccountNumber;

    public CreditRequestDTO(Long id, String accountNumber, String creditType, BigDecimal amount, Currency currency, String purpose, BigDecimal monthlySalary, String employmentStatus, int employmentPeriod, int repaymentPeriod, String branch, String phoneNumber, CreditRequestApproval approval, Long bankAccountNumber) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.creditType = creditType;
        this.amount = amount;
        this.currency = currency;
        this.purpose = purpose;
        this.monthlySalary = monthlySalary;
        this.employmentStatus = employmentStatus;
        this.employmentPeriod = employmentPeriod;
        this.repaymentPeriod = repaymentPeriod;
        this.branch = branch;
        this.phoneNumber = phoneNumber;
        this.approval = approval;
        this.bankAccountNumber = bankAccountNumber;
    }
}
