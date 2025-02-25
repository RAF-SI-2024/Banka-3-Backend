package rs.raf.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import rs.raf.user_service.entity.Employee;

import javax.validation.constraints.*;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
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

    @Pattern(regexp = "^\\+381(6[0-9]{1}|7[0-9]{1}|11)[0-9]{6,7}$", message = "Invalid phone number")
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

    public Employee mapToEmployee(){
        return new Employee(firstName, lastName, birthDate, gender, email, phone, address, username, position, department);
    }
}




