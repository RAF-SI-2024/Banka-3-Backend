package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.AccountType;

public class AccountMapper {

    public static AccountDto toDto(Account account, ClientDto client) {
        AccountDto dto = new AccountDto();
        dto.setAccountNumber(account.getAccountNumber());
        dto.setClientId(account.getClientId());
        if (account instanceof CompanyAccount) {
            dto.setCompanyId(((CompanyAccount) account).getCompanyId());
        }
        dto.setCreatedByEmployeeId(account.getCreatedByEmployeeId());
        dto.setCreationDate(account.getCreationDate() != null ? account.getCreationDate().toString() : null);
        dto.setExpirationDate(account.getExpirationDate() != null ? account.getExpirationDate().toString() : null);

        if (account.getCurrency() != null) {
            dto.setCurrencyCode(account.getCurrency().getCode());
        }

        dto.setStatus(account.getStatus());
        dto.setBalance(account.getBalance());
        dto.setAvailableBalance(account.getAvailableBalance());
        dto.setDailyLimit(account.getDailyLimit());
        dto.setMonthlyLimit(account.getMonthlyLimit());
        dto.setDailySpending(account.getDailySpending());
        dto.setMonthlySpending(account.getMonthlySpending());

        dto.setOwner(client);

        if (account instanceof CompanyAccount) {
            dto.setOwnershipType("poslovni");
        } else {
            dto.setOwnershipType("licni");
        }

        if (account.getType() == AccountType.CURRENT) {
            dto.setAccountCategory("tekuci");
        } else if (account.getType() == AccountType.FOREIGN) {
            dto.setAccountCategory("devizni");
        } else {
            dto.setAccountCategory("nepoznato");
        }

        return dto;
    }
}