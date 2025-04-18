package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.stock_service.domain.enums.TrackedPaymentStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackedPaymentDto {
    private Long id;
    private TrackedPaymentStatus status;
}
