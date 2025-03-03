package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class PayeeDto {
    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String accountNumber;
}