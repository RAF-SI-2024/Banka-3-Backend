package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyDto {
    private String code; // oznaka npr EUR
    private String name;
    private String symbol;
    private String countries;
    private String description;
    private boolean active;
}
