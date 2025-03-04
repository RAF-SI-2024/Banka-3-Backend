package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;

import javax.persistence.*;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class CreditRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String creditType;
    private BigDecimal amount;
    @ManyToOne
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
}
