package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class StockProfitResponseDto {
    private BigDecimal stockCommissionProfit;

}
