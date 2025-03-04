package rs.raf.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizedPersonelDto {
    private Long id;
    private String firstName;
    private String lastName;
    private Long dateOfBirth;
    private String gender;
    private String email;
    private String phoneNumber;
    private String address;
    private Long companyId;
}