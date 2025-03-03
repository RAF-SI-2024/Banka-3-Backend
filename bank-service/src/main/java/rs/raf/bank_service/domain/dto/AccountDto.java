package rs.raf.bank_service.domain.dto;

import lombok.Data;
import rs.raf.bank_service.domain.enums.AccountStatus;

import java.math.BigDecimal;

@Data
public class AccountDto {
    private String accountNumber;
    private Long clientId;
    private Long companyId;
    private Long createdByEmployeeId;

    private String creationDate;
    private String expirationDate;

    private String currencyCode;

    private AccountStatus status;

    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpending;
    private BigDecimal monthlySpending;

    private ClientDto owner;

    // ("lični" ili "poslovni")
    private String ownershipType;

    // ("tekući" ili "devizni")
    private String accountCategory;
}

