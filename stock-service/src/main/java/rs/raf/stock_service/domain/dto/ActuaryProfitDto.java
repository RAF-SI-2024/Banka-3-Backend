package rs.raf.stock_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActuaryProfitDto {
    private Long userId;
    private BigDecimal profit;
}
