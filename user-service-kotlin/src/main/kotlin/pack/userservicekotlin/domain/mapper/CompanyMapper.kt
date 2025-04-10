package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.company.CompanyResponseDto
import pack.userservicekotlin.domain.entities.Company

fun Company?.toDto(): CompanyResponseDto? {
    if (this == null) return null

    return CompanyResponseDto(
        id = id,
        name = name,
        registrationNumber = registrationNumber,
        taxId = taxId,
        activityCode = activityCode,
        address = address,
        majorityOwnerId = majorityOwner?.id,
    )
}
