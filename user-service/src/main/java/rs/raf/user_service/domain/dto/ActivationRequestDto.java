package rs.raf.user_service.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ActivationRequestDto {
    private String token;

    @NotNull(message = "Password cannot be null")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d.*\\d).*$", message = "Password must contain at least 2 numbers, 1 uppercase letter, and 1 lowercase letter")
    private String password;
}
