package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.AuthController;
import rs.raf.user_service.controller.UserController;
import rs.raf.user_service.dto.LoginRequestDTO;
import rs.raf.user_service.dto.LoginResponseDTO;
import rs.raf.user_service.service.AuthService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import rs.raf.user_service.dto.ActivationRequestDto;

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
        String email = "client@example.com";
        String password = "password";
        String token = "jwtTokenClient";

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        when(authService.authenticateClient(email, password)).thenReturn(token);

        ResponseEntity<LoginResponseDTO> responseEntity = authController.clientLogin(request);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertEquals(token, responseEntity.getBody().getToken());
        verify(authService, times(1)).authenticateClient(email, password);
    }

    @Test
    public void testEmployeeLogin_Success() {
        String email = "employee@example.com";
        String password = "password";
        String token = "jwtTokenEmployee";

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        when(authService.authenticateEmployee(email, password)).thenReturn(token);

        ResponseEntity<LoginResponseDTO> responseEntity = authController.employeeLogin(request);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertEquals(token, responseEntity.getBody().getToken());
        verify(authService, times(1)).authenticateEmployee(email, password);
    }

    @Test
    void testRequestPasswordReset_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");

        ResponseEntity<Void> response = authController.requestPasswordReset(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testRequestPasswordReset_Failure() {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");

        doThrow(new RuntimeException()).when(authService).requestPasswordReset(anyString());

        ResponseEntity<Void> response = authController.requestPasswordReset(request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testResetPassword_Success() {
        ActivationRequestDto dto = new ActivationRequestDto();
        dto.setToken("valid_token");
        dto.setPassword("newPassword");

        ResponseEntity<Void> response = authController.resetPassword(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testResetPassword_Failure() {
        ActivationRequestDto dto = new ActivationRequestDto();
        dto.setToken("invalid_token");
        dto.setPassword("newPassword");

        doThrow(new RuntimeException()).when(authService).resetPassword(anyString(), anyString());

        ResponseEntity<Void> response = authController.resetPassword(dto);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
