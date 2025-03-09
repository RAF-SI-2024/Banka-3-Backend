package rs.raf.bank_service.domain.dto;

import lombok.Data;

@Data
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