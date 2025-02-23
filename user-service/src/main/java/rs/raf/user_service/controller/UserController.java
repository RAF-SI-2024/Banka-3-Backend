
package rs.raf.user_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.entity.PermissionDTO;
import rs.raf.user_service.entity.UserPermissionDTO;
import rs.raf.user_service.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}/permissions")
    public ResponseEntity<List<PermissionDTO>> getUserPermissions(@PathVariable Long userId) {
        List<PermissionDTO> permissions = userService.getUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/{userId}/permissions")
    public ResponseEntity<Void> addPermissionToUser(@PathVariable Long userId, @RequestBody UserPermissionDTO userPermissionDTO) {
        userService.addPermissionToUser(userId, userPermissionDTO.getPermissionId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermissionFromUser(@PathVariable Long userId, @PathVariable Long permissionId) {
        userService.removePermissionFromUser(userId, permissionId);
        return ResponseEntity.ok().build();
    }
}