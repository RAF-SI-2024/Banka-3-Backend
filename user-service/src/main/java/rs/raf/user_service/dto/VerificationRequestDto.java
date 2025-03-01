package rs.raf.user_service.dto;

import lombok.*;

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
}

