package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActuaryDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String role;
    private BigDecimal profit;
}
