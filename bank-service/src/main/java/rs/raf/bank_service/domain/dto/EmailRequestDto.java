package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EmailRequestDto implements Serializable {
    // Getteri i setteri
    private String code;
    private String destination;

}
