package rs.raf.bank_service.domain.dto;

import lombok.Data;


@Data
public class Banka2AccountCurrencyDto {
    private String id;
    private Banka2AccountRefDto account;
    private Banka2EmployeeDto employee;
}
