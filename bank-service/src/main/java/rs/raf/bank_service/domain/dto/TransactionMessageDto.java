package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMessageDto {
    private String type;
    private String payloadJson;
    private Long userId;
    private Long timestamp;
}
