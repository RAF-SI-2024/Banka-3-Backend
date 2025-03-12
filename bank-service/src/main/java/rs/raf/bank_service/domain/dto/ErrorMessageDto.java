package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Data
@Getter
@Setter
public class ErrorMessageDto {
    private String error;
}
