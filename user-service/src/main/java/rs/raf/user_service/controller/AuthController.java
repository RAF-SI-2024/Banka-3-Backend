package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.user_service.entity.LoginRequest;
import rs.raf.user_service.entity.LoginResponse;
import rs.raf.user_service.service.AuthService;

@Tag(name = "Authentication Controller", description = "API for authenticating users")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Client Login", description = "Endpoint for logging client and generating JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Succesfully generated JWT token"),
            @ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    @PostMapping("/login/client")
    public ResponseEntity<LoginResponse> clientLogin(@RequestBody LoginRequest request) {
        String token = authService.authenticateClient(request.getEmail(), request.getPassword());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Employee Login", description = "Endpoint for logging employee and generating JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Succesfully generated JWT token"),
            @ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    @PostMapping("/login/employee")
    public ResponseEntity<LoginResponse> employeeLogin(@RequestBody LoginRequest request) {
        String token = authService.authenticateEmployee(request.getEmail(), request.getPassword());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
}
