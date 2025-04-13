package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExchangeDto {
    private String mic;
    private String name;
    private String acronym;
    private CountryDto polity;
    private String currencyCode;
    private Long timeZone;
    private boolean testMode;
}
