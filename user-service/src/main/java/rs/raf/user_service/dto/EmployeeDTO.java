package rs.raf.user_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String username;
    private String position;
    private String department;
    private boolean active;

    //from BaseUser
    private String firstName;
    private String lastName;
    private String email;

    private Date birthDate;
    private String gender;
    private String phone;
    private String address;



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
}