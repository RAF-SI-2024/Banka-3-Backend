package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyPaymentStatusDto {
    private boolean success;

    public boolean getSuccess() {
        return success;
    }
}
