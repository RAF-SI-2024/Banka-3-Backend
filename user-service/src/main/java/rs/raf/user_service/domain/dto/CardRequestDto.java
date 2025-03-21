package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardRequestDto {
    private String name;
    private String issuer;
    private String type;
    private String accountNumber;
}
