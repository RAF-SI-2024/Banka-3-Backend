package rs.raf.user_service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.configuration.JwtTokenUtil;
import rs.raf.user_service.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

public class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenUtil jwtTokenUtil;
    private AuthService authService;

    @BeforeEach
    public void setup() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenUtil);
    }

    @Test
    public void testAuthenticateEmployee_Success() {
        String email = "employee@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";
        String token = "jwtTokenEmployee";

        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPassword(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email)).thenReturn(token);

        String returnedToken = authService.authenticate(email, rawPassword);
        assertEquals(token, returnedToken);
    }

    @Test
    public void testAuthenticateClient_Success() {
        String email = "client@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";
        String token = "jwtTokenClient";

        Client client = new Client();
        client.setEmail(email);
        client.setPassword(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email)).thenReturn(token);

        String returnedToken = authService.authenticate(email, rawPassword);
        assertEquals(token, returnedToken);
    }

    @Test
    public void testAuthenticate_InvalidPassword() {
        String email = "test@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPassword(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.authenticate(email, rawPassword);
        });
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    public void testAuthenticate_UserNotFound() {
        String email = "nonexistent@example.com";
        String rawPassword = "password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.authenticate(email, rawPassword);
        });
        assertEquals("Invalid credentials", exception.getMessage());
    }
}
