package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
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
    // @Column(unique = true, length = 18)
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private String accountNumber;

    private Long clientId;
    private Long createdByEmployeeId;

    private LocalDate creationDate;
    private LocalDate expirationDate;

    @ManyToOne
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

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
}