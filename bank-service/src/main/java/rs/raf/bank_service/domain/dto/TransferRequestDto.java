package rs.raf.bank_service.domain.dto;

import lombok.Data;

@Data
public class TransferRequestDto {
    private Long userId;
    private Long fromAccountId;
    private Long toAccountId;
    private Double amount;
}
