package rs.raf.user_service.dto;

import lombok.Data;

@Data
public class ActivationRequestDto {
    private String token;
    private String password;
}
