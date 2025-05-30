package rs.raf.bank_service.domain.dto;

import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutePaymentDto {
    private Long clientId;
    private CreatePaymentDto createPaymentDto;
}

