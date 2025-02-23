package rs.raf.user_service;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import rs.raf.user_service.controller.AuthController;
import rs.raf.user_service.dtos.LoginRequest;
import rs.raf.user_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEmployeeLogin_Success() throws Exception {
        String email = "employee@example.com";
        String password = "password";
        String token = "jwtTokenEmployee";

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        when(authService.authenticate(email, password)).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    public void testClientLogin_Success() throws Exception {
        String email = "client@example.com";
        String password = "password";
        String token = "jwtTokenClient";

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        when(authService.authenticate(email, password)).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token));
    }
}

