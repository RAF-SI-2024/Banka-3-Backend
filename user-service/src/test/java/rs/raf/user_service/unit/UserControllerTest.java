package rs.raf.user_service.unit;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.UserController;
import rs.raf.user_service.domain.dto.RoleRequestDto;
import rs.raf.user_service.domain.dto.UserDto;
import rs.raf.user_service.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    public UserControllerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllUsers() {
        Page<UserDto> users = new PageImpl<>(List.of(new UserDto()));
        when(userService.listUsers(any(PageRequest.class))).thenReturn(users);

        ResponseEntity<Page<UserDto>> response = userController.getAllUsers(0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
    }

    @Test
    void testGetUserRole() {
        when(userService.getUserRole(1L)).thenReturn("ROLE_ADMIN");

        ResponseEntity<String> response = userController.getUserRole(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ROLE_ADMIN", response.getBody());
    }

    @Test
    void testAddRoleToUser_Success() {
        doNothing().when(userService).addRoleToUser(eq(1L), any(RoleRequestDto.class));

        RoleRequestDto roleRequestDto = new RoleRequestDto();
        ResponseEntity<Void> response = userController.addRoleToUser(1L, roleRequestDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testAddRoleToUser_NotFound() {
        doThrow(new RuntimeException()).when(userService).addRoleToUser(eq(1L), any(RoleRequestDto.class));

        RoleRequestDto roleRequestDto = new RoleRequestDto();
        ResponseEntity<Void> response = userController.addRoleToUser(1L, roleRequestDto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testRemoveRoleFromUser_Success() {
        doNothing().when(userService).removeRoleFromUser(1L, 2L);

        ResponseEntity<Void> response = userController.removeRoleFromUser(1L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testRemoveRoleFromUser_NotFound() {
        doThrow(new RuntimeException()).when(userService).removeRoleFromUser(1L, 2L);

        ResponseEntity<Void> response = userController.removeRoleFromUser(1L, 2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
