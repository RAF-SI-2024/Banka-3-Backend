package rs.raf.bank_service.domain.dto;


import lombok.*;

import java.util.List;

@Data
public class Banka2AccountItemDto {
    private String id;
    private String accountNumber;
    private String name;
    private Banka2ClientDto client;
    private double balance;
    private double availableBalance;
    private Banka2EmployeeDto employee;
    private Banka2CurrencyDto currency;
    private Banka2AccountTypeDto type;
    private List<Banka2AccountCurrencyDto> accountCurrencies;
    private double dailyLimit;
    private double monthlyLimit;
    private String creationDate;
    private String expirationDate;
    private boolean status;
    private String createdAt;
    private String modifiedAt;
}
