package pack.userservicekotlin.domain.dto.actuary_limit

import java.math.BigDecimal

data class ActuaryResponseDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val role: String,
    var profit: BigDecimal,
)
