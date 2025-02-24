package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import rs.raf.user_service.configuration.JwtTokenUtil;
import rs.raf.user_service.entity.Client;
import rs.raf.user_service.entity.Employee;
import rs.raf.user_service.entity.Permission;
import rs.raf.user_service.repository.ClientRepository;
import rs.raf.user_service.repository.EmployeeRepository;
import rs.raf.user_service.service.AuthService;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private PasswordEncoder passwordEncoder;
    private JwtTokenUtil jwtTokenUtil;
    private ClientRepository clientRepository;
    private EmployeeRepository employeeRepository;
    private AuthService authService;

    @BeforeEach
    public void setup() {
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        clientRepository = mock(ClientRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        authService = new AuthService(passwordEncoder, jwtTokenUtil, clientRepository, employeeRepository);
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
        Permission permission = new Permission();
        permission.setName("CLIENT");
        client.setPermissions(Collections.singleton(permission));

        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email, Collections.singletonList("CLIENT"))).thenReturn(token);

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
        Permission permission = new Permission();
        permission.setName("CLIENT");
        client.setPermissions(Collections.singleton(permission));

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
        employee.setEmail(email);
        employee.setPassword(encodedPassword);
        Permission permission = new Permission();
        permission.setName("EMPLOYEE");
        employee.setPermissions(Collections.singleton(permission));

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtTokenUtil.generateToken(email, Collections.singletonList("EMPLOYEE"))).thenReturn(token);

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
        Permission permission = new Permission();
        permission.setName("EMPLOYEE");
        employee.setPermissions(Collections.singleton(permission));

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
}
