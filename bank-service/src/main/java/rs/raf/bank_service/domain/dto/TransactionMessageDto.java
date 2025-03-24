package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.bank_service.domain.enums.TransactionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMessageDto {
    private TransactionType type;
    private String payloadJson;
    private Long userId;
    private Long timestamp;
}
