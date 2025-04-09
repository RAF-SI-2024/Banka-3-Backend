package pack.userservicekotlin.domain.dto.actuary_limit

import java.math.BigDecimal

data class ActuaryLimitResponseDto(
    val limitAmount: BigDecimal? = null,
    val usedLimit: BigDecimal? = null,
    val needsApproval: Boolean = false,
)
