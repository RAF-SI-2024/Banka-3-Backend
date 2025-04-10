package pack.userservicekotlin.domain.dto.external

data class CardRequestDto(
    val name: String? = null,
    val issuer: String? = null,
    val type: String? = null,
    val accountNumber: String? = null,
)
