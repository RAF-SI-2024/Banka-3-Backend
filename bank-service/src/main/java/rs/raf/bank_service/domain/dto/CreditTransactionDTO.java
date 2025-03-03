package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.entity.Credit;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreditTransactionDTO {
    private Long id;
    private Credit credit;

    private LocalDate transactionDate;
    private BigDecimal amount;
    private boolean isPaid;

    public CreditTransactionDTO(Long id, Credit credit, LocalDate transactionDate, BigDecimal amount, boolean isPaid) {
        this.id = id;
        this.credit = credit;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.isPaid = isPaid;
    }
}
