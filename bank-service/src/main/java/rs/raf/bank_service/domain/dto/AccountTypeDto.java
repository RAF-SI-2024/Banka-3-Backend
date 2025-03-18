package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.AccountOwnerType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeDto {
    private String accountNumber;
    private AccountOwnerType subtype;
}
