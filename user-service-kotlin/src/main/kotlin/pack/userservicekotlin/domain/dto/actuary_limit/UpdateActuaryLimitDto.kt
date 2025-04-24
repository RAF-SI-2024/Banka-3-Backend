package pack.userservicekotlin.domain.dto.actuary_limit

import java.math.BigDecimal

data class UpdateActuaryLimitDto(
    val newLimit: BigDecimal,
)
