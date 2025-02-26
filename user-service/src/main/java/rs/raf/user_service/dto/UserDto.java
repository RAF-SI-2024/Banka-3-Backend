package rs.raf.user_service.dto;


import lombok.Data;

import java.util.Date;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private Date birthDate;
    private String gender;
    private String email;
    private String phone;
    private String address;
}