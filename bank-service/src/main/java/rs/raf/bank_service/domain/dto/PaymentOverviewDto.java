package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter

public class PaymentOverviewDto {

    private Long id;
    private String senderName;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private TransactionStatus paymentStatus;
}
