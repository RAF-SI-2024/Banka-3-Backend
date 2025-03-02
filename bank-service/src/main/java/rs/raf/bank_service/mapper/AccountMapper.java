package rs.raf.bank_service.mapper;


import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.AccountTypeDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CurrentAccount;
import rs.raf.bank_service.domain.entity.ForeignCurrencyAccount;

@Component
public class AccountMapper {

    public AccountTypeDto toAccountTypeDto(Account account){
        if (account == null) return null;
        AccountTypeDto dto = new AccountTypeDto();
        dto.setAccountNumber(account.getAccountNumber());
        if (account instanceof CurrentAccount){
            dto.setSubtype(((CurrentAccount) account).getSubType().name());
        } else if (account instanceof ForeignCurrencyAccount) {
            dto.setSubtype(((ForeignCurrencyAccount) account).getSubType().name());
        }
        return dto;
    }
}
