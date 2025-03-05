package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.entity.Credit;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreditTransactionDto {
    private Credit credit;

    private LocalDate transactionDate;
    private BigDecimal amount;
    private boolean isPaid;

    public CreditTransactionDto(Credit credit, LocalDate transactionDate, BigDecimal amount, boolean isPaid) {
        this.credit = credit;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.isPaid = isPaid;
    }
}
