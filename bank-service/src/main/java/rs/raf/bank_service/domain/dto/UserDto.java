package rs.raf.bank_service.domain.dto;

import lombok.Data;


@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private String phone;
    private String gender;
    private String birthDate;
    private String jmbg;
}
