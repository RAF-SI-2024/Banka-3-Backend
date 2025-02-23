package rs.raf.user_service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.user_service.entity.PermissionDTO;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserPermissions_UserExists_ReturnsPermissions() {

        Long userId = 1L;
        Employee user = new Employee();
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setName("READ");
        user.setPermissions(new HashSet<>(Set.of(permission)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));


        List<PermissionDTO> permissions = userService.getUserPermissions(userId);


        assertNotNull(permissions);
        assertEquals(1, permissions.size());
        assertEquals("READ", permissions.get(0).getName());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserPermissions_UserNotFound_ThrowsException() {

        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserPermissions(userId);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void addPermissionToUser_UserAndPermissionExist_AddsPermission() {

        Long userId = 1L;
        Long permissionId = 2L;
        Employee user = new Employee();
        Permission permission = new Permission();
        permission.setId(permissionId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));


        userService.addPermissionToUser(userId, permissionId);


        assertTrue(user.getPermissions().contains(permission));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void addPermissionToUser_UserAlreadyHasPermission_ThrowsException() {

        Long userId = 1L;
        Long permissionId = 2L;
        Employee user = new Employee();
        Permission permission = new Permission();
        permission.setId(permissionId);
        user.setPermissions(new HashSet<>(Set.of(permission)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));


        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.addPermissionToUser(userId, permissionId);
        });

        assertEquals("User already has this permission", exception.getMessage());
        verify(userRepository, never()).save(user);
    }

    @Test
    void removePermissionFromUser_UserAndPermissionExist_RemovesPermission() {

        Long userId = 1L;
        Long permissionId = 2L;
        Employee user = new Employee();
        Permission permission = new Permission();
        permission.setId(permissionId);
        user.setPermissions(new HashSet<>(Set.of(permission)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));


        userService.removePermissionFromUser(userId, permissionId);


        assertFalse(user.getPermissions().contains(permission));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void removePermissionFromUser_UserDoesNotHavePermission_ThrowsException() {

        Long userId = 1L;
        Long permissionId = 2L;
        Employee user = new Employee();
        Permission permission = new Permission();
        permission.setId(permissionId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));


        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.removePermissionFromUser(userId, permissionId);
        });

        assertEquals("User does not have this permission", exception.getMessage());
        verify(userRepository, never()).save(user);
    }

    @Test
    void addPermissionToUser_PermissionNotFound_ThrowsException() {

        Long userId = 1L;
        Long permissionId = 2L;
        Employee user = new Employee();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.addPermissionToUser(userId, permissionId);
        });

        assertEquals("Permission not found", exception.getMessage());
        verify(userRepository, never()).save(user);
    }
}


