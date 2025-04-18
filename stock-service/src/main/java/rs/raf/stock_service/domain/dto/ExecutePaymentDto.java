package rs.raf.stock_service.domain.dto;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecutePaymentDto {
    private Long clientId;
    private CreatePaymentDto createPaymentDto;
}

