package rs.raf.user_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class ClientDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;   // ✅ Dodato polje username
    private String password;
    private String address;
    private String phone;      // ✅ Proveri da li postoji u DTO
    private String gender;     // ✅ Proveri da li postoji u DTO
    private Date birthDate;    // ✅ Proveri da li postoji u DTO

    public ClientDTO(Long id, String firstName, String lastName, String email,
                     String username, String password, String address,
                     String phone, String gender, Date birthDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.address = address;
        this.phone = phone;
        this.gender = gender;
        this.birthDate = birthDate;
    }
}
