package rs.raf.user_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private Long id;
    private String username;
    private String position;
    private String department;
    private boolean active;

    //from BaseUser
    private String firstName;
    private String lastName;
    private String email;
    private String jmbg;
    private Date birthDate;
    private String gender;
    private String phone;
    private String address;

}