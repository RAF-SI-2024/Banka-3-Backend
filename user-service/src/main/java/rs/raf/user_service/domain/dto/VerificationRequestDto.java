package rs.raf.user_service.domain.dto;

import lombok.*;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequestDto {
    private Long userId;
    private LocalDateTime expirationTime;
    private Long targetId;
    private VerificationStatus status;
    private VerificationType verificationType;
    private LocalDateTime createdAt;
}

