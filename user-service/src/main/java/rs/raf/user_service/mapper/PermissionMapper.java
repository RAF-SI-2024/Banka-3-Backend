package rs.raf.user_service.mapper;

import rs.raf.user_service.dto.PermissionDTO;
import rs.raf.user_service.entity.Permission;

public class PermissionMapper {

    public static PermissionDTO toDTO(Permission permission) {
        if (permission == null) {
            return null;
        }

        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        return dto;
    }

    public static Permission toEntity(PermissionDTO dto) {
        if (dto == null) {
            return null;
        }

        Permission permission = new Permission();
        permission.setId(dto.getId());
        permission.setName(dto.getName());
        return permission;
    }
}
