package rs.raf.user_service.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.raf.user_service.domain.dto.PermissionDto;
import rs.raf.user_service.domain.dto.PermissionRequestDto;
import rs.raf.user_service.domain.dto.RoleRequestDto;
import rs.raf.user_service.domain.dto.UserDto;
import rs.raf.user_service.domain.entity.BaseUser;
import rs.raf.user_service.domain.entity.Permission;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.domain.mapper.PermissionMapper;
import rs.raf.user_service.domain.mapper.UserMapper;
import rs.raf.user_service.repository.PermissionRepository;
import rs.raf.user_service.repository.RoleRepository;
import rs.raf.user_service.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public String getUserRole(Long userId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getRole().getName();
    }

    public void addRoleToUser(Long userId, RoleRequestDto roleRequestDto) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findById(roleRequestDto.getId())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (user.getRole().getName().equalsIgnoreCase(role.getName())) {
            throw new RuntimeException("User already has this role");
        }

        user.setRole(role);
        userRepository.save(user);
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (!user.getRole().getName().equalsIgnoreCase(role.getName())) {
            throw new RuntimeException("User does not have this role");
        }

        user.setRole(null);
        userRepository.save(user);
    }


    public Page<UserDto> listUsers(Pageable pageable) {
        Page<BaseUser> usersPage = userRepository.findAll(pageable);
        return userRepository.findAll(pageable)
                .map(UserMapper::toDto);
    }
}
