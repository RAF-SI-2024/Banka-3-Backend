package rs.raf.user_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.raf.user_service.configuration.JwtTokenUtil;
import rs.raf.user_service.dto.EmailRequestDto;
import rs.raf.user_service.entity.*;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.repository.UserRepository;


import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RabbitTemplate rabbitTemplate;

    public AuthService(PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil, ClientRepository clientRepository, EmployeeRepository employeeRepository,
                       AuthTokenRepository authTokenRepository, RabbitTemplate rabbitTemplate,UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.clientRepository = clientRepository;
        this.employeeRepository = employeeRepository;
        this.authTokenRepository = authTokenRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.userRepository = userRepository;
    }

    public String authenticateClient(String email, String password) {
        Client user = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        return jwtTokenUtil.generateToken(user.getEmail(),permissions);
    }

    public String authenticateEmployee(String email, String password) {
        Employee user = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        List<String> permissions = user.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());

        return jwtTokenUtil.generateToken(user.getEmail(),permissions);
    }
    public void requestPasswordReset(String email){
        BaseUser user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        UUID token = UUID.fromString(UUID.randomUUID().toString());
        EmailRequestDto emailRequestDto = new EmailRequestDto(token.toString(),email);
        rabbitTemplate.convertAndSend("reset-password",emailRequestDto);

        Long createdAt = System.currentTimeMillis();
        Long expiresAt = createdAt + 86400000;//24h
        AuthToken authToken = new AuthToken(createdAt, expiresAt, token.toString(), "reset-password",user.getId());
        authTokenRepository.save(authToken);

    }
    public void resetPassword(String token, String password){
        AuthToken currAuthToken = authTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Invalid token."));
        if(currAuthToken.getExpiresAt()>System.currentTimeMillis()){
            currAuthToken.setExpiresAt(System.currentTimeMillis());
            BaseUser user = userRepository.findById(currAuthToken.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            user.setPassword(passwordEncoder.encode(password));
            if(user instanceof Client client1){
                clientRepository.save(client1);
            } else if (user instanceof Employee employee) {
                employeeRepository.save(employee);
            }
        }else throw new RuntimeException("Expired token.");
    }
    public void checkToken(String token){
        AuthToken currAuthToken = authTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Invalid token."));
        if(currAuthToken.getExpiresAt()<System.currentTimeMillis())
            throw new RuntimeException("Expired token.");
    }
    public void setPassword(String token, String password){
        AuthToken currAuthToken = authTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Invalid token."));
        if(currAuthToken.getExpiresAt()>System.currentTimeMillis()) {
            BaseUser user = userRepository.findById(currAuthToken.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
            currAuthToken.setExpiresAt(Instant.now().toEpochMilli());
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }else throw new RuntimeException("Expired token");

    }

}
