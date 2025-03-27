package rs.raf.bank_service.domain.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AuthorizedPersonelDto {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String phoneNumber;
    private String address;
    private Long companyId;

    public AuthorizedPersonelDto(Long id, String firstName, String lastName, Long companyId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.companyId = companyId;


    }
}
