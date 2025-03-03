package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class PayeeDto {
    private Long id;

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Account number cannot be empty")
    private String accountNumber;

    @NotNull(message = "Client ID cannot be null") // ✅ Osigurava da clientId bude postavljen
    private Long clientId;
}
