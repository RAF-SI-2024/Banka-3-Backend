package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.raf.user_service.domain.dto.EmailRequestDto;
import rs.raf.user_service.domain.entity.AuthToken;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;
import rs.raf.user_service.repository.AuthTokenRepository;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.repository.UserRepository;
import rs.raf.user_service.service.AuthService;
import rs.raf.user_service.utils.JwtTokenUtil;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class AuthServiceTest {

    private PasswordEncoder passwordEncoder;
    private JwtTokenUtil jwtTokenUtil;
    private ClientRepository clientRepository;
    private EmployeeRepository employeeRepository;
    private UserRepository userRepository;
    private AuthTokenRepository authTokenRepository;
    private RabbitTemplate rabbitTemplate;

    private AuthService authService;

    @BeforeEach
    public void setup() {
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        clientRepository = mock(ClientRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        authTokenRepository = mock(AuthTokenRepository.class);
        userRepository = mock(UserRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        authService = new AuthService(passwordEncoder, jwtTokenUtil, clientRepository, employeeRepository, authTokenRepository, rabbitTemplate, userRepository);
    }

    @Test
    public void testAuthenticateClient_Success() {
        String email = "client@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";
        String token = "jwtTokenClient";

        Client client = new Client();
        client.setId(1L);
        client.setEmail(email);
        client.setPassword(encodedPassword);
        // Umesto dodele permisija, dodeljujemo ulogu:
        Role clientRole = new Role();
        clientRole.setName("CLIENT");
        client.setRole(clientRole);

        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email, 1L, "CLIENT")).thenReturn(token);

        String returnedToken = authService.authenticateClient(email, rawPassword);
        assertEquals(token, returnedToken);
    }

    @Test
    public void testAuthenticateClient_InvalidPassword() {
        String email = "client@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        Client client = new Client();
        client.setEmail(email);
        client.setPassword(encodedPassword);
        Role clientRole = new Role();
        clientRole.setName("CLIENT");
        client.setRole(clientRole);

        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        String returnedToken = authService.authenticateClient(email, rawPassword);
        assertNull(returnedToken, "Expected null when password is invalid");
    }

    @Test
    public void testAuthenticateClient_UserNotFound() {
        String email = "client@example.com";
        String rawPassword = "password";

        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        String returnedToken = authService.authenticateClient(email, rawPassword);
        assertNull(returnedToken, "Expected null when user is not found");
    }

    @Test
    public void testAuthenticateEmployee_Success() {
        String email = "employee@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";
        String token = "jwtTokenEmployee";

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmail(email);
        employee.setPassword(encodedPassword);
        Role employeeRole = new Role();
        employeeRole.setName("EMPLOYEE");
        employee.setRole(employeeRole);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email, 1L, "EMPLOYEE")).thenReturn(token);

        String returnedToken = authService.authenticateEmployee(email, rawPassword);
        assertEquals(token, returnedToken);
    }

    @Test
    public void testAuthenticateEmployee_InvalidPassword() {
        String email = "employee@example.com";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPassword(encodedPassword);
        Role employeeRole = new Role();
        employeeRole.setName("EMPLOYEE");
        employee.setRole(employeeRole);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        String returnedToken = authService.authenticateEmployee(email, rawPassword);
        assertNull(returnedToken, "Expected null when password is invalid");
    }

    @Test
    public void testAuthenticateEmployee_UserNotFound() {
        String email = "employee@example.com";
        String rawPassword = "password";

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        String returnedToken = authService.authenticateEmployee(email, rawPassword);
        assertNull(returnedToken, "Expected null when user is not found");
    }

    @Test
    public void testRequestPasswordReset_Success() {
        String email = "test@example.com";
        // Koristimo Client kao primer, ali može biti i Employee
        Client user = new Client();
        user.setId(1L);
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        authService.requestPasswordReset(email);

        verify(authTokenRepository).save(any(AuthToken.class));
        verify(rabbitTemplate).convertAndSend(eq("reset-password"), any(EmailRequestDto.class));
    }

    @Test
    public void testRequestPasswordReset_UserNotFound() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.requestPasswordReset(email);
        });
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testResetPassword_Success() {
        String token = "valid-token";
        String newPassword = "newPassword123";
        AuthToken authToken = new AuthToken();
        authToken.setExpiresAt(Instant.now().toEpochMilli() + 100000);
        // Koristimo Client kao BaseUser
        Client client = new Client();
        client.setId(1L);
        Role clientRole = new Role();
        clientRole.setName("CLIENT");
        client.setRole(clientRole);

        when(authTokenRepository.findByToken(token)).thenReturn(Optional.of(authToken));
        when(userRepository.findById(authToken.getUserId())).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");

        authService.resetPassword(token, newPassword);
        // Verifikujemo da je client sačuvan (ovo zavisi od toga koju logiku koristite – ovde proveravamo klijentski slučaj)
        verify(clientRepository).save(any(Client.class));
        // Token expiration treba da se ažurira
        assertTrue(authToken.getExpiresAt() <= Instant.now().toEpochMilli());
    }

    @Test
    public void testResetPassword_InvalidToken() {
        String token = "invalid-token";
        String newPassword = "newPassword123";
        when(authTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(token, newPassword);
        });
        assertEquals("Invalid token.", exception.getMessage());
    }

    @Test
    public void testResetPassword_ExpiredToken() {
        String token = "expired-token";
        String newPassword = "newPassword123";
        AuthToken authToken = new AuthToken();
        authToken.setExpiresAt(Instant.now().toEpochMilli() - 100000);
        when(authTokenRepository.findByToken(token)).thenReturn(Optional.of(authToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(token, newPassword);
        });
        assertEquals("Expired token.", exception.getMessage());
    }

    @Test
    public void testSetPassword_Success() {
        String token = "valid-token";
        String password = "newPassword123";
        AuthToken authToken = new AuthToken();
        authToken.setExpiresAt(Instant.now().plusSeconds(3600).toEpochMilli());
        authToken.setUserId(1L);

        Client client = new Client();
        client.setId(1L);
        Role clientRole = new Role();
        clientRole.setName("CLIENT");
        client.setRole(clientRole);

        when(authTokenRepository.findByToken(token)).thenReturn(Optional.of(authToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

        authService.setPassword(token, password);

        verify(userRepository, times(1)).save(client);
        assertEquals("encodedPassword", client.getPassword());
        assertTrue(authToken.getExpiresAt() <= Instant.now().toEpochMilli());
    }

    @Test
    public void testSetPassword_InvalidToken() {
        String token = "invalid-token";

        when(authTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.setPassword(token, "newPassword123");
        });
        assertEquals("Invalid token.", exception.getMessage());
    }

    @Test
    public void testSetPassword_ExpiredToken() {
        String token = "expired-token";
        AuthToken authToken = new AuthToken();
        authToken.setExpiresAt(Instant.now().minusSeconds(3600).toEpochMilli());

        when(authTokenRepository.findByToken(token)).thenReturn(Optional.of(authToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.setPassword(token, "newPassword123");
        });
        assertEquals("Expired token", exception.getMessage());
    }
}
