package rs.raf.bank_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class TaxDto {
    private String senderAccountNumber;
    private BigDecimal amount;
    private Long clientId;
}
