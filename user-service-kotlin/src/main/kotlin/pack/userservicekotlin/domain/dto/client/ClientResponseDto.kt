package pack.userservicekotlin.domain.dto.client

import java.util.*

data class ClientResponseDto(
    val id: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val birthDate: Date? = null,
    val jmbg: String? = null,
    val username: String? = null,
)
