package pack.userservicekotlin.domain.dto.employee

import java.util.*

data class EmployeeResponseDto(
    val id: Long? = null,
    val username: String? = null,
    val position: String? = null,
    val department: String? = null,
    val active: Boolean = false,
    // from BaseUser
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val jmbg: String? = null,
    val birthDate: Date? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val role: String? = null,
)
