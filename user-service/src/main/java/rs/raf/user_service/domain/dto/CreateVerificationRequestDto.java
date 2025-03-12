package rs.raf.user_service.domain.dto;

import lombok.*;
import rs.raf.user_service.domain.enums.VerificationType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVerificationRequestDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long targetId;
    @NotNull
    private VerificationType verificationType;
    @NotNull
    @NotBlank
    private String details;
}

