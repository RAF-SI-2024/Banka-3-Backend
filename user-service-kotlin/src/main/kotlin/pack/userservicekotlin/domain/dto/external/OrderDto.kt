package pack.userservicekotlin.domain.dto.external

import java.math.BigDecimal

data class OrderDto(
    val userId: Long? = null,
    val profit: BigDecimal? = null,
)
