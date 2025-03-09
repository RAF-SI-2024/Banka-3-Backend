package rs.raf.user_service.domain.mapper;

import rs.raf.user_service.domain.dto.RoleDto;
import rs.raf.user_service.domain.entity.Role;

import java.util.stream.Collectors;

public class RoleMapper {

    public static RoleDto toDto(Role role) {
        if (role == null) return null;
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                    .map(PermissionMapper::toDTO)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
}
