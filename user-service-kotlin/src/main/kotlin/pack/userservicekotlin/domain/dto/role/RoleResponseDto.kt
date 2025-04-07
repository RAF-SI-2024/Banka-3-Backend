package pack.userservicekotlin.domain.dto.role

import pack.userservicekotlin.domain.dto.permission.PermissionResponseDto

data class RoleResponseDto(
    val id: Long? = null,
    val name: String? = null,
    val permissions: Set<PermissionResponseDto>? = null,
)
