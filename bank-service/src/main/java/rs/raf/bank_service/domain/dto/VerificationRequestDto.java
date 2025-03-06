package rs.raf.bank_service.domain.dto;

import lombok.*;
import rs.raf.bank_service.domain.enums.VerificationStatus;
import rs.raf.bank_service.domain.enums.VerificationType;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequestDto {

    private Long userId;
    private String email;
    private String code;
    private LocalDateTime expirationTime;
    private int attempts;
    private Long targetId;

    private VerificationStatus status;
    private VerificationType verificationType;
}
