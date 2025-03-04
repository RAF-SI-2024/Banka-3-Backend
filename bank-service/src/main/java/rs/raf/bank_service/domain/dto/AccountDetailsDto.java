package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.AccountType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsDto {

    //private String accountName;
    private String accountNumber;
    private String AccountOwner;
    private AccountType accountType;
    private BigDecimal availableBalance;
    private BigDecimal reservedFunds;
    private BigDecimal balance;

    public AccountDetailsDto(String accountNumber, AccountType accountType, BigDecimal availableBalance, BigDecimal reservedFunds,
                             BigDecimal balance){
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.availableBalance = availableBalance;
        this.reservedFunds = reservedFunds;
        this.balance = balance;
    }
}
