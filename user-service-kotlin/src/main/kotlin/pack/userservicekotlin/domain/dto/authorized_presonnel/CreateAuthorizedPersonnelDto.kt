package pack.userservicekotlin.domain.dto.authorized_presonnel

import jakarta.validation.constraints.*
import java.time.LocalDate

data class CreateAuthorizedPersonnelDto(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String? = null,
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String? = null,
    @NotNull(message = "Date of birth is required")
    val dateOfBirth:
        @Past(message = "Date of birth must be in the past")
        LocalDate? = null,
    @NotBlank(message = "Gender is required")
    val gender: String? = null,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    val email: String? = null,
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0?[1-9][0-9]{6,14}$", message = "Invalid phone number")
    val phoneNumber: String? = null,
    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 100, message = "Address must be between 5 and 100 characters")
    val address: String? = null,
    @NotNull(message = "Company ID is required")
    val companyId:
        @Min(value = 1, message = "Company ID must be a positive number")
        Long? = null,
)
