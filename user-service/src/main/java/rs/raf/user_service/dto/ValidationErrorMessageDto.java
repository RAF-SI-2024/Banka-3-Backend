package rs.raf.user_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class ValidationErrorMessageDto {
    private List<String> errors;
}
