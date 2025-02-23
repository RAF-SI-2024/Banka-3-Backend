package rs.raf.user_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import rs.raf.user_service.controller.AuthController;
import rs.raf.user_service.entity.LoginRequest;
import rs.raf.user_service.entity.LoginResponse;
import rs.raf.user_service.service.AuthService;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        String email = "client@example.com";
        String password = "password";
        String token = "jwtTokenClient";

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(authService.authenticateClient(email, password)).thenReturn(token);

        ResponseEntity<LoginResponse> responseEntity = authController.clientLogin(request);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertEquals(token, responseEntity.getBody().getToken());
        verify(authService, times(1)).authenticateClient(email, password);
    }

    @Test
    public void testEmployeeLogin_Success() {
        String email = "employee@example.com";
        String password = "password";
        String token = "jwtTokenEmployee";

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(authService.authenticateEmployee(email, password)).thenReturn(token);

        ResponseEntity<LoginResponse> responseEntity = authController.employeeLogin(request);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertEquals(token, responseEntity.getBody().getToken());
        verify(authService, times(1)).authenticateEmployee(email, password);
    }
}
