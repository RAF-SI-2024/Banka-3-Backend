package rs.raf.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Data
@AllArgsConstructor
public class EmailRequestDto implements Serializable {
    // Getteri i setteri
    private String code;
    private String destination;

}
