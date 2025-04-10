package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.role.RoleResponseDto
import pack.userservicekotlin.domain.entities.Role

fun Role?.toDto(): RoleResponseDto? {
    if (this == null) return null

    return RoleResponseDto(
        id = id,
        name = name,
        permissions = permissions.mapNotNull { it.toDto() }.toSet(),
    )
}
