package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.dto.PermissionDto;
import rs.raf.user_service.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "User Permissions", description = "API for managing user permissions")
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/{userId}/permissions")
    @Operation(summary = "Get user permissions", description = "Returns a list of all permissions for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<PermissionDto>> getUserPermissions(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId) {
        List<PermissionDto> permissions = userService.getUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/{userId}/permissions")
    @Operation(summary = "Add permission to user", description = "Adds a permission to a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission added successfully"),
            @ApiResponse(responseCode = "404", description = "User or permission not found"),
            @ApiResponse(responseCode = "400", description = "User already has this permission")
    })
    public ResponseEntity<Void> addPermissionToUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Permission ID", required = true, example = "2")
            @RequestBody Long permissionId) {
        try {
            userService.addPermissionToUser(userId, permissionId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/{userId}/permissions/{permissionId}")
    @Operation(summary = "Remove permission from user", description = "Removes a permission from a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission removed successfully"),
            @ApiResponse(responseCode = "404", description = "User or permission not found"),
            @ApiResponse(responseCode = "400", description = "User does not have this permission")
    })
    public ResponseEntity<Void> removePermissionFromUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Permission ID", required = true, example = "2")
            @PathVariable Long permissionId) {
        try {
            userService.removePermissionFromUser(userId, permissionId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}