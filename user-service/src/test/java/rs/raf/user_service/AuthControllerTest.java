package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.AuthController;
import rs.raf.user_service.controller.UserController;
import rs.raf.user_service.dto.ActivationRequestDto;
import rs.raf.user_service.dto.LoginRequestDto;
import rs.raf.user_service.dto.LoginResponseDto;
import rs.raf.user_service.dto.RequestPasswordResetDto;
import rs.raf.user_service.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    private AuthService authService;
    private AuthController authController;
    private UserController userController;

    @BeforeEach
    public void setup() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);
        userController = new UserController();
    }

    @Test
    public void testClientLogin_Success() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("password");

        String expectedToken = "valid_jwt_token";
        when(authService.authenticateClient(request.getEmail(), request.getPassword())).thenReturn(expectedToken);

        ResponseEntity<?> responseEntity = authController.clientLogin(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertInstanceOf(LoginResponseDto.class, responseEntity.getBody());

        LoginResponseDto responseDTO = (LoginResponseDto) responseEntity.getBody();
        assertEquals(expectedToken, responseDTO.getToken());
    }

    @Test
    public void testClientLogin_InvalidCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("invalid@example.com");
        request.setPassword("wrongpassword");

        when(authService.authenticateClient(request.getEmail(), request.getPassword())).thenReturn(null);

        ResponseEntity<?> responseEntity = authController.clientLogin(request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals("Bad credentials", responseEntity.getBody());
    }

    @Test
    public void testEmployeeLogin_Success() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("password");

        String expectedToken = "valid_jwt_token";
        when(authService.authenticateEmployee(request.getEmail(), request.getPassword())).thenReturn(expectedToken);

        ResponseEntity<?> responseEntity = authController.employeeLogin(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertInstanceOf(LoginResponseDto.class, responseEntity.getBody());

        LoginResponseDto responseDTO = (LoginResponseDto) responseEntity.getBody();
        assertEquals(expectedToken, responseDTO.getToken());
    }

    @Test
    public void testEmployeeLogin_InvalidCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("invalid@example.com");
        request.setPassword("wrongpassword");

        when(authService.authenticateEmployee(request.getEmail(), request.getPassword())).thenReturn(null);

        ResponseEntity<?> responseEntity = authController.employeeLogin(request);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals("Bad credentials", responseEntity.getBody());
    }

    @Test
    void testRequestPasswordReset_Success() {
        RequestPasswordResetDto requestPasswordResetDTO = new RequestPasswordResetDto();
        requestPasswordResetDTO.setEmail("test@example.com");

        ResponseEntity<Void> response = authController.requestPasswordReset(requestPasswordResetDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testRequestPasswordReset_Failure() {
        RequestPasswordResetDto requestPasswordResetDTO = new RequestPasswordResetDto();
        requestPasswordResetDTO.setEmail("test@example.com");

        doThrow(new RuntimeException()).when(authService).requestPasswordReset(anyString());

        ResponseEntity<Void> response = authController.requestPasswordReset(requestPasswordResetDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testResetPassword_Success() {
        ActivationRequestDto dto = new ActivationRequestDto();
        dto.setToken("valid_token");
        dto.setPassword("newPassword");

        ResponseEntity<Void> response = authController.activateUser(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testActivateUser_Success() {
        ActivationRequestDto request = new ActivationRequestDto();
        request.setToken("valid-token");
        request.setPassword("newPassword123");

        ResponseEntity<Void> response = authController.activateUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Provera da li je servis pozvan
        verify(authService, times(1)).setPassword("valid-token", "newPassword123");
    }

    @Test
    public void testActivateUser_InvalidToken() {
        ActivationRequestDto request = new ActivationRequestDto();
        request.setToken("invalid-token");
        request.setPassword("newPassword123");

        doThrow(new RuntimeException("Invalid token.")).when(authService).setPassword("invalid-token", "newPassword123");

        ResponseEntity<Void> response = authController.activateUser(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(authService, times(1)).setPassword("invalid-token", "newPassword123");
    }

    @Test
    public void testActivateUser_ExpiredToken() {
        ActivationRequestDto request = new ActivationRequestDto();
        request.setToken("expired-token");
        request.setPassword("newPassword123");

        doThrow(new RuntimeException("Expired token.")).when(authService).setPassword("expired-token", "newPassword123");

        ResponseEntity<Void> response = authController.activateUser(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(authService, times(1)).setPassword("expired-token", "newPassword123");
    }
}
