package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.AccountStatus;
import rs.raf.bank_service.domain.enums.AccountType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
@AllArgsConstructor
public abstract class Account {
    @Id
    @Column(updatable = false)
    private String accountNumber;

    private String name;

    private Long clientId;
    private Long createdByEmployeeId;

    private LocalDate creationDate;
    private LocalDate expirationDate;


    @ManyToOne(fetch = FetchType.EAGER)
    private Currency currency;

    // active/inactive
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    // current/foreign
    @Enumerated(EnumType.STRING)
    private AccountType type;
    // personal/company...

    @Enumerated(EnumType.STRING)
    private AccountOwnerType accountOwnerType;

    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpending;
    private BigDecimal monthlySpending;


    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Card> cards = new ArrayList<>();

    public Account(Long clientId, Long createdByEmployeeId, LocalDate creationDate, LocalDate expirationDate,
                   Currency currency, AccountStatus status, AccountType type, AccountOwnerType accountOwnerType,
                   BigDecimal balance, BigDecimal availableBalance, BigDecimal dailyLimit, BigDecimal monthlyLimit,
                   BigDecimal dailySpending, BigDecimal monthlySpending) {

        this.clientId = clientId;
        this.createdByEmployeeId = createdByEmployeeId;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;
        this.currency = currency;
        this.status = status;
        this.type = type;
        this.accountOwnerType = accountOwnerType;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.dailySpending = dailySpending;
        this.monthlySpending = monthlySpending;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountNumber='" + accountNumber + '\'' +
                ", clientId=" + clientId +
                ", createdByEmployeeId=" + createdByEmployeeId +
                ", creationDate=" + creationDate +
                ", expirationDate=" + expirationDate +
                ", currency=" + currency +
                ", status=" + status +
                ", type=" + type +
                ", accountOwnerType=" + accountOwnerType +
                ", balance=" + balance +
                ", availableBalance=" + availableBalance +
                ", dailyLimit=" + dailyLimit +
                ", monthlyLimit=" + monthlyLimit +
                ", dailySpending=" + dailySpending +
                ", monthlySpending=" + monthlySpending +
                ", cards=" + cards +
                '}';
    }
}
