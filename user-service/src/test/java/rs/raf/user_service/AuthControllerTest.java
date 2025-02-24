package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.AuthController;
import rs.raf.user_service.dto.LoginRequestDTO;
import rs.raf.user_service.dto.LoginResponseDTO;
import rs.raf.user_service.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    private AuthService authService;
    private AuthController authController;

    @BeforeEach
    public void setup() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);
    }

    @Test
    public void testClientLogin_Success() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password");

        String expectedToken = "valid_jwt_token";
        when(authService.authenticateClient(request.getEmail(), request.getPassword())).thenReturn(expectedToken);

        ResponseEntity<?> responseEntity = authController.clientLogin(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertInstanceOf(LoginResponseDTO.class, responseEntity.getBody());

        LoginResponseDTO responseDTO = (LoginResponseDTO) responseEntity.getBody();
        assertEquals(expectedToken, responseDTO.getToken());
    }

    @Test
    public void testClientLogin_InvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("invalid@example.com");
        request.setPassword("wrongpassword");

        when(authService.authenticateClient(request.getEmail(), request.getPassword())).thenReturn(null);

        ResponseEntity<?> responseEntity = authController.clientLogin(request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals("Bad credentials", responseEntity.getBody());
    }

    @Test
    public void testEmployeeLogin_Success() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@example.com");
        request.setPassword("password");

        String expectedToken = "valid_jwt_token";
        when(authService.authenticateEmployee(request.getEmail(), request.getPassword())).thenReturn(expectedToken);

        ResponseEntity<?> responseEntity = authController.employeeLogin(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertInstanceOf(LoginResponseDTO.class, responseEntity.getBody());

        LoginResponseDTO responseDTO = (LoginResponseDTO) responseEntity.getBody();
        assertEquals(expectedToken, responseDTO.getToken());
    }

    @Test
    public void testEmployeeLogin_InvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("invalid@example.com");
        request.setPassword("wrongpassword");

        when(authService.authenticateEmployee(request.getEmail(), request.getPassword())).thenReturn(null);

        ResponseEntity<?> responseEntity = authController.employeeLogin(request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals("Bad credentials", responseEntity.getBody());
    }
}
