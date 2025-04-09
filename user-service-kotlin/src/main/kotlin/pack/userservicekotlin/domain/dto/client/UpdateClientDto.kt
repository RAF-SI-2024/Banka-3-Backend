package pack.userservicekotlin.domain.dto.client

import jakarta.validation.constraints.*

class UpdateClientDto(
    @NotNull(message = "Last name cannot be null")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters") val lastName: String? = null,
    @NotNull(message = "Gender cannot be null")
    @Size(min = 1, max = 1, message = "Gender must be 1 character")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Gender must be a letter") val gender: String? = null,
    @Pattern(regexp = "^0?[1-9][0-9]{6,14}$", message = "Invalid phone number") val phone: String? = null,
    @NotNull(message = "Address cannot be null") val address: String? = null,
)
