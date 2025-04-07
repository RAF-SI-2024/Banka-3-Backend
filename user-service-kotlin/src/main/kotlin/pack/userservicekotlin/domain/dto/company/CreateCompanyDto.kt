package pack.userservicekotlin.domain.dto.company

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateCompanyDto(
    @NotNull(message = "Name must not be null")
    @NotBlank
    val name: String? = null,
    @NotNull(message = "registrationNumber must not be null")
    @NotBlank
    val registrationNumber: String? = null,
    @NotNull(message = "taxId must not be null")
    @NotBlank
    val taxId: String? = null,
    @NotNull(message = "activityCode must not be null")
    @NotBlank
    val activityCode: String? = null,
    @NotNull(message = "address must not be null")
    @NotBlank
    val address: String? = null,
    @NotNull(message = "majorityOwner must not be null")
    val majorityOwner:
        @Min(1L)
        Long? = null,
)
