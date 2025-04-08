package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsDto {

    private String name;
    private String accountNumber;
    private String AccountOwner;
    private BigDecimal availableBalance;
    private BigDecimal reservedFunds;
    private BigDecimal balance;
    private String currencyCode;

}
