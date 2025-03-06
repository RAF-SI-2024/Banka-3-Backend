package rs.raf.user_service.domain.dto;

import lombok.*;
import rs.raf.user_service.domain.enums.VerificationType;

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

