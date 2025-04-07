package pack.userservicekotlin.domain.dto.activity_code

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ActivationRequestDto(
    val token: String? = null,
    @NotNull(message = "Password cannot be null")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d.*\\d).*$",
        message = "Password must contain at least 2 numbers, 1 uppercase letter, and 1 lowercase letter",
    )
    val password: String? = null,
)
