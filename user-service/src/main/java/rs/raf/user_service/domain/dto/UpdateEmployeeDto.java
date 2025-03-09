package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
public class UpdateEmployeeDto {

    @NotNull(message = "Last name cannot be null")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    private String lastName;

    @NotNull(message = "Gender cannot be null")
    @Size(min = 1, max = 1, message = "Gender must be 1 character")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Gender must be a letter")
    private String gender;

    @Pattern(regexp = "^0?[1-9][0-9]{6,14}$", message = "Invalid phone number")
    private String phone;

    @NotNull(message = "Address cannot be null")
    private String address;

    @NotNull(message = "Position cannot be null")
    @Size(min = 2, max = 100, message = "Position must be between 2 and 100 characters")
    private String position;

    @NotNull(message = "Department cannot be null")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;
}




