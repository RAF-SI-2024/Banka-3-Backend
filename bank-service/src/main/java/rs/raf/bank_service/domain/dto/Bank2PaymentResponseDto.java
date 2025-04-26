package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bank2PaymentResponseDto {
    private Boolean success;
    private String message;
}