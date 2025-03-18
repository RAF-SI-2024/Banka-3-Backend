package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ChangeAgentLimitDto {
    private BigDecimal newLimit;
}
