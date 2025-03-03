package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardDto {
    /*@NotBlank
    @Size(min = 16, max = 16)
    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank
    @Size(min = 3, max = 3)
    @Pattern(regexp = "\\d{3}", message = "CVV must be 3 digits")
    private String cvv;*/

    @NotBlank
    private String type;

    @NotBlank
    private String name;

    @NotBlank
    private String accountNumber;

    //@NotBlank
    //private String status;

    @NotNull
    private BigDecimal cardLimit;
}
