package rs.raf.user_service.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.*;
import java.util.Date;

@Getter
@Setter
public class EmployeeDTO {

    private Long id;
    private boolean active;

    @NotNull(message = "Username cannot be null")
    @Size(min = 2, max = 100, message = "Username must be between 2 and 100 characters")
    private String username;

    @NotNull(message = "Position cannot be null")
    @Size(min = 2, max = 100, message = "Position must be between 2 and 100 characters")
    private String position;

    @NotNull(message = "Department cannot be null")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;

    //from BaseUser
    @NotNull(message = "First name cannot be null")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    private String firstName;

    @NotNull(message = "Last name cannot be null")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    private String lastName;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Gender cannot be null")
    @Size(min = 1, max = 1, message = "Gender must be 1 character")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Gender must be a letter")
    private String gender;

    @Pattern(regexp = "^\\+381(6[0-9]{1}|7[0-9]{1}|11)[0-9]{6,7}$", message = "Invalid phone number")
    private String phone;

    @NotNull(message = "Address cannot be null")
    private String address;

    @Past(message = "Date of birth must be in the past")
    private Date birthDate;

    public EmployeeDTO(String firstName, String lastName, String email,
                       String username, String position, String department, boolean active) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.username = username;
        this.position = position;
        this.department = department;
        this.active = active;
    }

    public EmployeeDTO(String firstName, String lastName, Date birthDate, String gender, String email, String phone,
                       String address, String username, String position, String department) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.username = username;
        this.position = position;
        this.department = department;
    }
}




