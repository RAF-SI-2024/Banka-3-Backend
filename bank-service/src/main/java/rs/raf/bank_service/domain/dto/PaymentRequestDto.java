package rs.raf.bank_service.domain.dto;

import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long fromAccountId;
    private Long toAccountId;
    private Double amount;

}
