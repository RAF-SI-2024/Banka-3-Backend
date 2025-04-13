package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BankProfitResponseDto {
    private BigDecimal exchangeProfit;
}
