package rs.raf.user_service.unit;

import org.junit.jupiter.api.Test;
import rs.raf.user_service.domain.dto.PermissionDto;
import rs.raf.user_service.domain.dto.RoleDto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoleDtoTest {

    @Test
    void testGettersAndSetters() {
        PermissionDto permission1 = new PermissionDto();
        PermissionDto permission2 = new PermissionDto();
        permission2.setId(2L);
        permission2.setName("READ_PRIVILEGES");
        permission1.setId(1L);
        permission1.setName("READ_PRIVILEGES");

        RoleDto roleDto = new RoleDto();
        roleDto.setId(10L);
        roleDto.setName("ADMIN");
        roleDto.setPermissions(Set.of(permission1, permission2));

        assertEquals(10L, roleDto.getId());
        assertEquals("ADMIN", roleDto.getName());
        assertEquals(2, roleDto.getPermissions().size());
        assertEquals("READ_PRIVILEGES", roleDto.getPermissions().stream().findFirst().get().getName());
    }
}
