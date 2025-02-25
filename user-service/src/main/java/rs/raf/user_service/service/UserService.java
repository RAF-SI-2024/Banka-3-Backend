package rs.raf.user_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.EmailRequestDto;
import rs.raf.user_service.dto.PermissionDto;
import rs.raf.user_service.dto.UserDto;
import rs.raf.user_service.entity.AuthToken;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.mapper.PermissionMapper;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.repository.PermissionRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;


    @Autowired
    public UserService(UserRepository userRepository, PermissionRepository permissionRepository,AuthTokenRepository authTokenRepository, RabbitTemplate rabbitTemplate, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.authTokenRepository = authTokenRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public List<PermissionDto> getUserPermissions(Long userId) {
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
}