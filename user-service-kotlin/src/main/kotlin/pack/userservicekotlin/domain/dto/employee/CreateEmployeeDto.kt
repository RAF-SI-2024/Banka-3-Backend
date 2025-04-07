package pack.userservicekotlin.domain.dto.employee

import jakarta.validation.constraints.*
import jakarta.validation.constraints.Past
import java.util.*

data class CreateEmployeeDto(
    @NotNull(message = "First name cannot be null")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters") var firstName: String? = null,
    @NotNull(message = "Last name cannot be null")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters") var lastName: String? = null,
    var birthDate:
        @Past(message = "Date of birth must be in the past")
        Date? = null,
    @NotNull(message = "Gender cannot be null")
    @Size(min = 1, max = 1, message = "Gender must be 1 character")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Gender must be a letter") var gender: String? = null,
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be varid") var email: String? = null,
    @NotNull(message = "Active cannot be null") var active: Boolean? = null,
    @Pattern(regexp = "^0?[1-9][0-9]{6,14}$", message = "Invarid phone number") var phone: String? = null,
    @NotNull(message = "Address cannot be null") var address: String? = null,
    @NotNull(message = "Username cannot be null")
    @Size(min = 2, max = 100, message = "Username must be between 2 and 100 characters") var username: String? = null,
    @NotNull(message = "Position cannot be null")
    @Size(min = 2, max = 100, message = "Position must be between 2 and 100 characters") var position: String? = null,
    @NotNull(message = "Department cannot be null")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters") var department: String? = null,
    @NotNull(message = "Jmbg cannot be null")
    @Pattern(regexp = "^[0-9]{13}$", message = "Jmbg must be exactly 13 digits") var jmbg: String? = null,
    // EMPLOYEE, ADMIN, SUPERVISOR, AGENT
    @NotNull(message = "Role cannot be null") var role: String? = null,
)
