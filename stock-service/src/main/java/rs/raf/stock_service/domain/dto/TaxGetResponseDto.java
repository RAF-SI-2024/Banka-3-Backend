package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class TaxGetResponseDto {
    private BigDecimal paidForThisYear;
    private BigDecimal unpaidForThisMonth;

    public TaxGetResponseDto() {
        paidForThisYear = BigDecimal.ZERO;
        unpaidForThisMonth = BigDecimal.ZERO;
    }
}
