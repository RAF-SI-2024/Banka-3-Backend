package rs.raf.user_service.mapper;

import rs.raf.user_service.dto.PermissionDto;
import rs.raf.user_service.entity.Permission;

public class PermissionMapper {

    public static PermissionDto toDTO(Permission permission) {
        if (permission == null) {
            return null;
        }

        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        return dto;
    }

    public static Permission toEntity(PermissionDto dto) {
        if (dto == null) {
            return null;
        }

        Permission permission = new Permission();
        permission.setId(dto.getId());
        permission.setName(dto.getName());
        return permission;
    }
}
