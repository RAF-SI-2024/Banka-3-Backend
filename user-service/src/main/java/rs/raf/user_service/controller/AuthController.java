package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.user_service.dto.LoginRequestDTO;
import rs.raf.user_service.dto.LoginResponseDTO;
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
    public ResponseEntity<?> clientLogin(@RequestBody LoginRequestDTO request) {
        String token = authService.authenticateClient(request.getEmail(), request.getPassword());
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials");
        }
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Employee Login", description = "Endpoint for logging employee and generating JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Succesfully generated JWT token"),
            @ApiResponse(responseCode = "401", description = "Bad credentials")
    })
    @PostMapping("/login/employee")
    public ResponseEntity<?> employeeLogin(@RequestBody LoginRequestDTO request) {
        String token = authService.authenticateEmployee(request.getEmail(), request.getPassword());
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials");
        }
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
}
