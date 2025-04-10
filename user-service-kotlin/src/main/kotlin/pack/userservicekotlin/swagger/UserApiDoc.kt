package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.external.UserResponseDto
import pack.userservicekotlin.domain.dto.role.RoleRequestDto

@Tag(name = "User Roles", description = "API for managing user roles")
interface UserApiDoc {
    @Operation(summary = "Get all users with pagination")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        ],
    )
    @GetMapping
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<UserResponseDto>>

    @Operation(summary = "Get user role", description = "Returns a role for a specific user.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Role retrieved successfully"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ],
    )
    @GetMapping("/{userId}/role")
    fun getUserRole(
        @Parameter(description = "User ID", required = true, example = "1")
        @PathVariable userId: Long,
    ): ResponseEntity<String>

    @Operation(summary = "Add role to user", description = "Adds a role to a user.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Role added successfully"),
            ApiResponse(responseCode = "404", description = "User or role not found"),
            ApiResponse(responseCode = "400", description = "User already has this role"),
        ],
    )
    @PostMapping("/{userId}/role")
    fun addRoleToUser(
        @PathVariable userId: Long,
        @RequestBody @Valid roleRequestDto: RoleRequestDto,
    ): ResponseEntity<Void>

    @Operation(summary = "Remove role from user", description = "Removes a role from a user.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Role removed successfully"),
            ApiResponse(responseCode = "404", description = "User or role not found"),
            ApiResponse(responseCode = "400", description = "User does not have this role"),
        ],
    )
    @DeleteMapping("/{userId}/role/{roleId}")
    fun removeRoleFromUser(
        @Parameter(description = "User ID", required = true, example = "1")
        @PathVariable userId: Long,
        @Parameter(description = "Role ID", required = true, example = "2")
        @PathVariable roleId: Long,
    ): ResponseEntity<Void>
}
