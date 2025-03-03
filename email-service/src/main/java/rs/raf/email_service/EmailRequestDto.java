package rs.raf.email_service;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class EmailRequestDto implements Serializable {
    // Getteri i setteri
    private String code;
    private String destination;

}
