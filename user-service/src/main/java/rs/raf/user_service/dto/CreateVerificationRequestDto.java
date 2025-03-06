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
public class CreateVerificationRequestDto {
    private Long userId;
    private Long targetId;
    private VerificationType verificationType;
}

