package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.permission.PermissionRequestDto
import pack.userservicekotlin.domain.dto.permission.PermissionResponseDto
import pack.userservicekotlin.domain.entities.Permission

fun Permission?.toDto(): PermissionResponseDto? {
    if (this == null) return null

    return PermissionResponseDto(
        id = id,
        name = name,
    )
}

fun PermissionRequestDto?.toEntity(): Permission? {
    if (this == null) return null

    val permission = Permission()
    permission.id = id
    permission.name = name
    return permission
}
