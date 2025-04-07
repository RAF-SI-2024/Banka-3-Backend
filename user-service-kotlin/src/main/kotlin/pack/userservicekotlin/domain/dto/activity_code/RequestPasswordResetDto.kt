package pack.userservicekotlin.domain.dto.activity_code

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull

data class RequestPasswordResetDto(
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    val email: String? = null,
)
