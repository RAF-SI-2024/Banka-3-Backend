package rs.raf.user_service.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class LoginRequestDto {
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    private String password;
}
