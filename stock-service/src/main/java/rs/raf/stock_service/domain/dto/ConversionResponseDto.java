package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConversionResponseDto {
    private BigDecimal conversionRate;
    private BigDecimal convertedAmount;
}
