package pack.userservicekotlin.domain.dto.company

data class CompanyResponseDto(
    val id: Long? = null,
    val name: String? = null,
    val registrationNumber: String? = null,
    val taxId: String? = null,
    val activityCode: String? = null,
    val address: String? = null,
    val majorityOwnerId: Long? = null,
)
