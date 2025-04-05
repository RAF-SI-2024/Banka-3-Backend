package rs.raf.user_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.raf.user_service.domain.dto.RoleRequestDto;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.repository.RoleRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserService userService;

    // Uklonjena je metoda setUp() koja je ručno otvarala mokove.

    @Test
    void getUserRole_UserExists_ReturnsRoleName() {
        Long userId = 1L;
        Employee user = new Employee();
        Role role = new Role();
        role.setId(10L);
        role.setName("ADMIN");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String roleName = userService.getUserRole(userId);

        assertNotNull(roleName);
        assertEquals("ADMIN", roleName);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserRole_UserNotFound_ThrowsException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserRole(userId);
        });
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void addRoleToUser_UserAndRoleExist_AddsRole() {
        Long userId = 1L;
        Long roleId = 2L;
        Employee user = new Employee();
        // User initially has role CLIENT
        Role clientRole = new Role();
        clientRole.setId(1L);
        clientRole.setName("CLIENT");
        user.setRole(clientRole);

        Role newRole = new Role();
        newRole.setId(roleId);
        newRole.setName("EMPLOYEE");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(newRole));

        RoleRequestDto roleRequestDto = new RoleRequestDto(roleId);
        userService.addRoleToUser(userId, roleRequestDto);

        assertEquals("EMPLOYEE", user.getRole().getName());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void addRoleToUser_UserAlreadyHasSameRole_ThrowsException() {
        Long userId = 1L;
        Long roleId = 2L;
        Employee user = new Employee();
        Role role = new Role();
        role.setId(roleId);
        role.setName("EMPLOYEE");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.addRoleToUser(userId, new RoleRequestDto(roleId));
        });
        assertEquals("User already has this role", exception.getMessage());
        verify(userRepository, never()).save(user);
    }

    @Test
    void removeRoleFromUser_UserHasRole_RemovesRole() {
        Long userId = 1L;
        Long roleId = 2L;
        Employee user = new Employee();
        Role role = new Role();
        role.setId(roleId);
        role.setName("EMPLOYEE");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        userService.removeRoleFromUser(userId, roleId);

        assertNull(user.getRole());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void removeRoleFromUser_UserDoesNotHaveRole_ThrowsException() {
        Long userId = 1L;
        Long roleId = 2L;
        Employee user = new Employee();
        // User has a different role, e.g., CLIENT
        Role clientRole = new Role();
        clientRole.setId(3L);
        clientRole.setName("CLIENT");
        user.setRole(clientRole);

        Role roleToRemove = new Role();
        roleToRemove.setId(roleId);
        roleToRemove.setName("EMPLOYEE");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleToRemove));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.removeRoleFromUser(userId, roleId);
        });
        assertEquals("User does not have this role", exception.getMessage());
        verify(userRepository, never()).save(user);
    }


    @Test
    void testAddRoleToUser_UserNotFound() {
        Long userId = 1L;
        Long roleId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            userService.addRoleToUser(userId, new RoleRequestDto(roleId));
        });
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void testAddRoleToUser_RoleNotFound() {
        Long userId = 1L;
        Long roleId = 2L;

        Employee user = new Employee();
        user.setId(userId);
        user.setRole(new Role(1L, "CLIENT", null));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            userService.addRoleToUser(userId, new RoleRequestDto(roleId));
        });
        assertEquals("Role not found", ex.getMessage());
    }

    @Test
    void testRemoveRoleFromUser_UserNotFound() {
        Long userId = 1L;
        Long roleId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            userService.removeRoleFromUser(userId, roleId);
        });
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void testRemoveRoleFromUser_RoleNotFound() {
        Long userId = 1L;
        Long roleId = 2L;

        Employee user = new Employee();
        user.setId(userId);
        Role currentRole = new Role();
        currentRole.setId(3L);
        currentRole.setName("CLIENT");
        user.setRole(currentRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            userService.removeRoleFromUser(userId, roleId);
        });
        assertEquals("Role not found", ex.getMessage());
    }


}