package rs.raf.email_service;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmailRequestDto {
    // Getteri i setteri
    private String code;
    private String destination;

}
