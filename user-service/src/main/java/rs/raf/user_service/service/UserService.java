package rs.raf.user_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.raf.user_service.dto.EmailRequestDto;
import rs.raf.user_service.dto.PermissionDTO;
import rs.raf.user_service.dto.UserDTO;
import rs.raf.user_service.entity.AuthToken;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.mapper.PermissionMapper;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.repository.PermissionRepository;

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
    public void createUser(UserDTO userDto) {

        Client client = new Client();
        client.setFirstName(userDto.getFirstName());
        client.setLastName(userDto.getLastName());
        client.setBirthDate(userDto.getBirthDate());
        client.setGender(userDto.getGender());
        client.setEmail(userDto.getEmail());
        client.setPhone(userDto.getPhone());
        client.setAddress(userDto.getAddress());
        userRepository.save(client);

        UUID token = UUID.fromString(UUID.randomUUID().toString());
        EmailRequestDto emailRequestDto = new EmailRequestDto(token.toString(),client.getEmail());
        System.out.println(emailRequestDto.getDestination()+" "+emailRequestDto.getCode()+" evo me");
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.convertAndSend("activate-client-account",emailRequestDto);

        Long createdAt = System.currentTimeMillis();
        Long expiresAt = createdAt + 86400000;//24h
        AuthToken authToken = new AuthToken(createdAt, expiresAt, token.toString(), "activate-client-account",client.getId());
        authTokenRepository.save(authToken);;
    }
    //TODO dodaj endpoint za proveru tokena da li je validan
    public void activateUser(String token, String password){
        AuthToken currAuthToken = authTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Invalid token."));
        if(currAuthToken.getExpiresAt()>System.currentTimeMillis()){
            currAuthToken.setExpiresAt(System.currentTimeMillis());
            BaseUser client = userRepository.findById(currAuthToken.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            client.setPassword(passwordEncoder.encode(password));
            userRepository.save(client);
        }else throw new RuntimeException("Expired token");
    }
}