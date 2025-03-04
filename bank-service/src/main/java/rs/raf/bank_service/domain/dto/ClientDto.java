package rs.raf.bank_service.domain.dto;


import lombok.Data;

@Data
public class ClientDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
