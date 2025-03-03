package rs.raf.bank_service.domain.dto;
import lombok.Data;

@Data
public class EmailRequestDto {
    private String destination;
    private String code;
}
