package rs.raf.bank_service.domain.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationRequestDto implements Serializable {
    private Long userId;
    private Long targetId;  // Ovo je transactionId
    private String type;    //("Transfer"/ "Payment");
}
