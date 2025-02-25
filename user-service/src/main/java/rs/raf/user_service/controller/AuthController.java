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
import rs.raf.user_service.dto.ActivationRequestDto;
import rs.raf.user_service.dto.LoginRequestDTO;
import rs.raf.user_service.dto.LoginResponseDTO;
import rs.raf.user_service.service.AuthService;

import java.util.Map;

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
    public ResponseEntity<LoginResponseDTO> clientLogin(@RequestBody LoginRequestDTO request) {
        String token = authService.authenticateClient(request.getEmail(), request.getPassword());
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
    public ResponseEntity<LoginResponseDTO> employeeLogin(@RequestBody LoginRequestDTO request) {
        String token = authService.authenticateEmployee(request.getEmail(), request.getPassword());
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/request-password-reset")
    @Operation(summary = "Request password reset", description = "Requests password reset with email adress.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request for password reset successfully sent."),
            @ApiResponse(responseCode = "400", description = "Invalid email.")
    })
    public ResponseEntity<Void> requestPasswordReset(@RequestBody Map<String, String> request){
        String email = request.get("email");
        try {
            this.authService.requestPasswordReset(email);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password", description = "Resets password with email adress.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New password set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token.")
    })
    public ResponseEntity<Void> resetPassword(@RequestBody ActivationRequestDto activationRequestDto){
        try {
            authService.resetPassword(activationRequestDto.getToken(),activationRequestDto.getPassword());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @PostMapping("check-token")
    @Operation(summary = "Checks if a token is still valid", description = "Checks if a token is still valid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Valid"),
            @ApiResponse(responseCode = "404", description = "Invalid")
    })
    public ResponseEntity<Void> checkToken(@RequestBody Map<String, String> request){
        String token = request.get("token");
        try {
            authService.checkToken(token);
            return ResponseEntity.ok().build();
        }catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }
}
