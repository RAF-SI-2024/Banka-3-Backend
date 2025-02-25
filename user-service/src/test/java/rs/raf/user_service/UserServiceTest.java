package rs.raf.user_service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.raf.user_service.dto.PermissionDTO;
import rs.raf.user_service.dto.UserDTO;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.mapper.UserMapper;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.service.UserService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserMapper userMapper;

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

    //  TEST: Listanje korisnika sa paginacijom
    @Test
    void listUsers_ShouldReturnPagedUsers() {
        BaseUser user = mock(BaseUser.class);
        UserDTO userDTO = new UserDTO();
        Page<BaseUser> userPage = new PageImpl<>(Collections.singletonList(user));

        when(userRepository.findAll(PageRequest.of(0, 5))).thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(userDTO);

        List<UserDTO> result = userService.listUsers(0, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAll(PageRequest.of(0, 5));
        verify(userMapper, times(1)).toDto(user);
    }

    // TEST: Dohvatanje korisnika po ID-u - Success
    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        Long userId = 1L;
        BaseUser user = mock(BaseUser.class);
        UserDTO userDTO = new UserDTO();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userDTO, result);
        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).toDto(user);
    }

    // TEST: Dohvatanje korisnika po ID-u - Not Found
    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.getUserById(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    // TEST: Brisanje korisnika - Success
    @Test
    void deleteUser_ShouldDeleteUser_WhenExists() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    //  TEST: Brisanje korisnika - Not Found
    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> userService.deleteUser(userId));
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    public void addUser_Success() {
        UserDTO userDTO = new UserDTO(null, "Petar", "Petrović", "petar.p@example.com",
                "petar90", true, "Manager", "HR");
        Employee employee = new Employee();
        employee.setId(1L);

        when(userMapper.toEntity(userDTO)).thenReturn(employee);
        when(userRepository.save(any(Employee.class))).thenReturn(employee);
        when(userMapper.toDto(any(Employee.class))).thenReturn(userDTO);

        UserDTO createdUser = userService.addUser(userDTO);

        assertNotNull(createdUser);
        assertEquals("Petar", createdUser.getFirstName());
        verify(userRepository, times(1)).save(any(Employee.class));
    }

    @Test
    public void updateUser_Success() {
        Long userId = 1L;
        UserDTO userDTO = new UserDTO(userId, "Marko", "Marković", "marko.m@example.com",
                "marko90", true, "Developer", "IT");
        Employee employee = new Employee();
        employee.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
        when(userRepository.save(any(Employee.class))).thenReturn(employee);
        when(userMapper.toDto(any(Employee.class))).thenReturn(userDTO);

        UserDTO updatedUser = userService.updateUser(userId, userDTO);

        assertNotNull(updatedUser);
        assertEquals("Marko", updatedUser.getFirstName());
        verify(userRepository, times(1)).save(any(Employee.class));
    }
}


