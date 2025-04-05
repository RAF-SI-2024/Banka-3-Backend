package rs.raf.bank_service.domain.dto;

import lombok.Data;

import java.util.List;


@Data
public class Banka2CurrencyDto {
    private String id;
    private String name;
    private String code;
    private String symbol;
    private List<Banka2CountryDto> countries;
    private String description;
    private boolean status;
    private String createdAt;
    private String modifiedAt;
}

