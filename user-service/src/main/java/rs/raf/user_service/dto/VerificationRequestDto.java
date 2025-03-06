package rs.raf.user_service.dto;

import lombok.*;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.enums.VerificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequestDto {
    private Long userId;
    private String email;
    private LocalDateTime expirationTime;
    private int attempts;
    private Long targetId;

    private VerificationStatus status;
    private VerificationType verificationType;
}

