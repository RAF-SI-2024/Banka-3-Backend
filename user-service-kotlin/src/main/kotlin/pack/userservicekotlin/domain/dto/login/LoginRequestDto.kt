package pack.userservicekotlin.domain.dto.login

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull

data class LoginRequestDto(
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    val email: String? = null,
    @NotNull(message = "Email cannot be null")
    val password: String? = null,
)
