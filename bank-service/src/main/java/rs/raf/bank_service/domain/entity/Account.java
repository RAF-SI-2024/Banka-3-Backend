package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.AccountStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Account {
    @Id
    @Column(unique = true, length = 18)
    private String accountNumber;

    private Long clientId;
    private Long companyId;
    private Long createdByEmployeeId;
    
    private LocalDate creationDate;
    private LocalDate expirationDate;
    
    @ManyToOne
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpending;
    private BigDecimal monthlySpending;
}