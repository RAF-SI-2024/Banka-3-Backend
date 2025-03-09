package rs.raf.user_service.domain.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
public class RequestPasswordResetDto {
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;
}
