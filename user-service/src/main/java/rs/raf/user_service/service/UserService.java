package rs.raf.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.PermissionDTO;
import rs.raf.user_service.dto.UserDTO;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.mapper.PermissionMapper;
import rs.raf.user_service.mapper.UserMapper;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper; // ✅

    @Autowired
    public UserService(UserRepository userRepository, PermissionRepository permissionRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.userMapper = userMapper;
    }

    public List<PermissionDTO> getUserPermissions(Long userId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getPermissions().stream()
                .map(PermissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void addPermissionToUser(Long userId, Long permissionId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        if (user.getPermissions().contains(permission)) {
            throw new RuntimeException("User already has this permission");
        }

        user.getPermissions().add(permission);
        userRepository.save(user);
    }

    public void removePermissionFromUser(Long userId, Long permissionId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        if (!user.getPermissions().contains(permission)) {
            throw new RuntimeException("User does not have this permission");
        }

        user.getPermissions().remove(permission);
        userRepository.save(user);
    }


    //  Listanje korisnika sa paginacijom
    public List<UserDTO> listUsers(int page, int size) {
        Page<BaseUser> usersPage = userRepository.findAll(PageRequest.of(page, size));
        return usersPage.stream().map(userMapper::toDto).collect(Collectors.toList());
    }

    //  Dohvatanje detalja korisnika po ID-u
    public UserDTO getUserById(Long id) {
        BaseUser user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + id));
        return userMapper.toDto(user);
    }

    //  Brisanje korisnika
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    public UserDTO addUser(UserDTO userDTO) {
        BaseUser user = userMapper.toEntity(userDTO);
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) {
        BaseUser existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setEmail(userDTO.getEmail());

        //  Provera da li je Employee da bismo pristupili dodatnim poljima
        if (existingUser instanceof Employee employee) {
            employee.setUsername(userDTO.getUsername());
            employee.setActive(userDTO.getActive());
        }

        userRepository.save(existingUser);
        return userMapper.toDto(existingUser);
    }


}
