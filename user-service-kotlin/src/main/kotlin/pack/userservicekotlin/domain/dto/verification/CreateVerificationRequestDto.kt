package pack.userservicekotlin.domain.dto.verification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import pack.userservicekotlin.domain.enums.VerificationType

data class CreateVerificationRequestDto(
    @NotNull
    val userId: Long? = null,
    @NotNull
    val targetId: Long? = null,
    @NotNull
    val verificationType: VerificationType? = null,
    @NotNull
    @NotBlank
    val details: String? = null,
)
