package rs.raf.user_service.unit;

import org.junit.jupiter.api.Test;
import rs.raf.user_service.domain.dto.PermissionDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PermissionDtoTest {

    @Test
    void testGettersAndSetters() {
        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setId(1L);
        permissionDto.setName("READ_PRIVILEGES");

        assertEquals(1L, permissionDto.getId());
        assertEquals("READ_PRIVILEGES", permissionDto.getName());
    }
}
