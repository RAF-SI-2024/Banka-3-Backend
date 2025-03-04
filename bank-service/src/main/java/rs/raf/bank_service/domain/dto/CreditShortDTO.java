package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditShortDTO {
    private Long id;
    private String accountNumber;
    private BigDecimal amount;
    private String creditType;

    public CreditShortDTO(Long id, String accountNumber, BigDecimal amount, String creditType) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.creditType = creditType;
    }
}
