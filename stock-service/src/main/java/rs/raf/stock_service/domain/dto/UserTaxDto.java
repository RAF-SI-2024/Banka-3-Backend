package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTaxDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String role;
    private BigDecimal unpaidTaxThisMonth;
    private BigDecimal paidTaxThisYear;
}
