package rs.raf.bank_service.mapper;


import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.AccountTypeDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.PersonalAccount;

@Component
public class AccountMapper {

    public AccountTypeDto toAccountTypeDto(Account account){
        if (account == null) return null;
        AccountTypeDto dto = new AccountTypeDto();
        dto.setAccountNumber(account.getAccountNumber());
        if (account instanceof PersonalAccount){
            dto.setSubtype("Personal");
        } else if (account instanceof CompanyAccount) {
            dto.setSubtype("Company");
        }
        return dto;
    }
}
