package rs.raf.user_service.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.raf.user_service.domain.dto.PermissionDto;
import rs.raf.user_service.domain.dto.PermissionRequestDto;
import rs.raf.user_service.domain.dto.UserDto;
import rs.raf.user_service.domain.entity.BaseUser;
import rs.raf.user_service.domain.entity.Permission;
import rs.raf.user_service.domain.mapper.PermissionMapper;
import rs.raf.user_service.domain.mapper.UserMapper;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public List<PermissionDto> getUserPermissions(Long userId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getPermissions().stream()
                .map(PermissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void addPermissionToUser(Long userId, PermissionRequestDto permissionRequestDto) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Permission permission = permissionRepository.findById(permissionRequestDto.getId())
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

    public Page<UserDto> listUsers(Pageable pageable) {
        Page<BaseUser> usersPage = userRepository.findAll(pageable);
        return userRepository.findAll(pageable)
                .map(UserMapper::toDto);
    }
}
