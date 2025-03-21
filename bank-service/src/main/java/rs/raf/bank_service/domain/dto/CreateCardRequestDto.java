package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardType;

@Getter
@Setter
public class CreateCardRequestDto {
    private Long clientId;
    private String accountNumber;
    private CardType cardType;
    private CardIssuer cardIssuer;
    private String reason;
}
