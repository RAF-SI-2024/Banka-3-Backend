package pack.userservicekotlin.domain.dto.external

import java.io.Serializable

data class EmailRequestDto(
    val code: String? = null,
    val destination: String? = null,
) : Serializable
