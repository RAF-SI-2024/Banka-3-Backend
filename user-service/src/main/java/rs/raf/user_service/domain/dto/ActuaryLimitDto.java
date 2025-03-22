package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ActuaryLimitDto {
    private BigDecimal limitAmount;
    private BigDecimal usedLimit;
    private boolean needsApproval;
}
