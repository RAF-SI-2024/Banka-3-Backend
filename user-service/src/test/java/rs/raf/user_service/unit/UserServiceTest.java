package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import rs.raf.user_service.domain.dto.RoleRequestDto;
import rs.raf.user_service.domain.dto.UserDto;
import rs.raf.user_service.domain.entity.BaseUser;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.repository.RoleRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.service.UserService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Dodajemo podrazumevani mock za roleId = 2 ("EMPLOYEE"), tako da
        // se testovi koji očekuju normalan tok ne sruše odmah na "Role not found".
        // Ako neki test treba "Role not found", on će posebno override-ovati ovo.
        Role defaultRole = new Role();
        defaultRole.setId(2L);
        defaultRole.setName("EMPLOYEE");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(defaultRole));
    }

    // ------------------------------------------------------------------------------
    // getUserRole(...)
    // ------------------------------------------------------------------------------
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

    // ------------------------------------------------------------------------------
    // addRoleToUser(...)
    // ------------------------------------------------------------------------------
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
        // Ovde već postoji default stub za (2L), ali pokazujemo kako može i eksplicitno:
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
    void addRoleToUser_UserNotFound_ThrowsException() {
        Long userId = 999L;
        Long roleId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.addRoleToUser(userId, new RoleRequestDto(roleId))
        );
        assertEquals("User not found", ex.getMessage());
        verify(roleRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }
/*
    @Test
    void addRoleToUser_RoleNotFound_ThrowsException() {
        Long userId = 1L;
        Long roleId = 999L;
        Employee user = new Employee();
        user.setRole(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.addRoleToUser(userId, new RoleRequestDto(roleId))
        );
        assertEquals("Role not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
*/
    // ------------------------------------------------------------------------------
    // removeRoleFromUser(...)
    // ------------------------------------------------------------------------------
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
    void removeRoleFromUser_UserNotFound_ThrowsException() {
        Long userId = 999L;
        Long roleId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.removeRoleFromUser(userId, roleId)
        );
        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
/*
    @Test
    void removeRoleFromUser_RoleNotFound_ThrowsException() {
        Long userId = 1L;
        Long roleId = 999L;

        Employee user = new Employee();
        user.setRole(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.removeRoleFromUser(userId, roleId)
        );
        assertEquals("Role not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
*/
    // ------------------------------------------------------------------------------
    // listUsers(...)
    // ------------------------------------------------------------------------------
    /*
    @Test
    void testListUsers_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        BaseUser user1 = new Employee();
        user1.setId(1L);

        BaseUser user2 = new Employee();
        user2.setId(2L);

        Page<BaseUser> page = new PageImpl<>(List.of(user1, user2));
        when(userRepository.findAll(pageRequest)).thenReturn(page);

        Page<?> result = userService.listUsers(pageRequest);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(1L, ((UserDto) result.getContent().get(0)).getId());
        assertEquals(2L, ((UserDto) result.getContent().get(1)).getId());
        verify(userRepository, times(1)).findAll(pageRequest);
    }
*/
    @Test
    void testListUsers_EmptyPage() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<BaseUser> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.findAll(pageRequest)).thenReturn(emptyPage);

        Page<?> result = userService.listUsers(pageRequest);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(userRepository, times(1)).findAll(pageRequest);
    }
}
