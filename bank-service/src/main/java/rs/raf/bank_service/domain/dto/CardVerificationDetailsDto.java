package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardVerificationDetailsDto {
    private String name;
    private CardIssuer issuer;
    private CardType type;
    private String accountNumber;
}
