package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditShortDto {

    private String accountNumber;
    private BigDecimal amount;
    private String creditType;

    public CreditShortDto(String accountNumber, BigDecimal amount, String creditType) {

        this.accountNumber = accountNumber;
        this.amount = amount;
        this.creditType = creditType;
    }
}
