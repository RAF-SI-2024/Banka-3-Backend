package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.domain.dto.RoleRequestDto;
import rs.raf.user_service.domain.dto.UserDto;
import rs.raf.user_service.service.UserService;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "User roles", description = "API for managing user roles")
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all users with pagination")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Users retrieved successfully")})
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.listUsers(pageable));
    }
    //Ne koristiti, setovanje role-a za zaposlenog je prebaceno u update employee. Klijent nam jos uvek nema setovanje role, uvek je CLIENT
    //Verovatno ce biti obrisano
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/role")
    @Operation(summary = "Get user role", description = "Returns a role for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<String> getUserRole(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserRole(userId));
    }
    //Ne koristiti, setovanje role-a za zaposlenog je prebaceno u update employee. Klijent nam jos uvek nema setovanje role, uvek je CLIENT
    //Verovatno ce biti obrisano
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{userId}/role")
    @Operation(summary = "Add role to user", description = "Adds a role to a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role added successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "400", description = "User already has this role")
    })
    public ResponseEntity<Void> addRoleToUser(
            @PathVariable Long userId,
            @RequestBody @Valid RoleRequestDto roleRequestDto) {
        try {
            userService.addRoleToUser(userId, roleRequestDto);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    //Ne koristiti, setovanje role-a za zaposlenog je prebaceno u update employee. Klijent nam jos uvek nema setovanje role, uvek je CLIENT
    //Verovatno ce biti obrisano
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}/role/{roleId}")
    @Operation(summary = "Remove role from user", description = "Removes a role from a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "400", description = "User does not have this role")
    })
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Role ID", required = true, example = "2")
            @PathVariable Long roleId) {
        try {
            userService.removeRoleFromUser(userId, roleId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
