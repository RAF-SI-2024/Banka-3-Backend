package rs.raf.bank_service.domain.dto;

import lombok.Data;


@Data
public class Banka2ClientDto {
    private String id;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private int gender;
    private String uniqueIdentificationNumber;
    private String email;
    private String phoneNumber;
    private String address;
    private int role;
    private String createdAt;
    private String modifiedAt;
    private boolean activated;
}
