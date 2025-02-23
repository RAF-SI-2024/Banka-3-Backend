package rs.raf.user_service.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPermissionDTO {
    private Long userId;
    private Long permissionId;
}
