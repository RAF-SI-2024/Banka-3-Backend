package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateEmployeeDto {

    @NotNull(message = "First name cannot be null")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    private String firstName;

    @NotNull(message = "Last name cannot be null")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    private Date birthDate;

    @NotNull(message = "Gender cannot be null")
    @Size(min = 1, max = 1, message = "Gender must be 1 character")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Gender must be a letter")
    private String gender;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Active cannot be null")
    private Boolean active;

    @Pattern(regexp = "^0?[1-9][0-9]{6,14}$", message = "Invalid phone number")
    private String phone;

    @NotNull(message = "Address cannot be null")
    private String address;

    @NotNull(message = "Username cannot be null")
    @Size(min = 2, max = 100, message = "Username must be between 2 and 100 characters")
    private String username;

    @NotNull(message = "Position cannot be null")
    @Size(min = 2, max = 100, message = "Position must be between 2 and 100 characters")
    private String position;

    @NotNull(message = "Department cannot be null")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;

    @NotNull(message = "Jmbg cannot be null")
    @Pattern(
            regexp = "^[0-9]{13}$",
            message = "Jmbg must be exactly 13 digits"
    )
    private String jmbg;
}




