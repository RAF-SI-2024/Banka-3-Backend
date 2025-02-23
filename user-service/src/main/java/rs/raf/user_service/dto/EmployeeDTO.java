package rs.raf.user_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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




