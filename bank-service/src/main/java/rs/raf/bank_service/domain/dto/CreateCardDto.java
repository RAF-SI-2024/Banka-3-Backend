package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardDto {
    @NotNull
    private CardType type;

    @NotNull
    private CardIssuer issuer;

    @NotNull
    @NotBlank
    private String name;

    @NotNull
    @NotBlank
    private String accountNumber;

    @NotNull
    private BigDecimal cardLimit;
}
