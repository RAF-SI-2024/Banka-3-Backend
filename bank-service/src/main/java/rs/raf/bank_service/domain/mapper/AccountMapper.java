package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.*;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.enums.AccountType;
import java.math.BigDecimal;

@Component
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

    // ✅ Mapiranje iz Account u AccountDetailsDto (bez naziva vlasnika, to se setuje naknadno)
    public static AccountDetailsDto toDetailsDto(Account account) {
        if (account == null) return null;
        return new AccountDetailsDto(
                account.getAccountNumber(),
                account.getType(),
                account.getAvailableBalance(),
                BigDecimal.ZERO,
                account.getBalance()

        );
    }

    // ✅ Mapiranje iz Account u AccountDetailsDto (bez naziva vlasnika, to se setuje naknadno)
    public static CompanyAccountDetailsDto toCompanyDetailsDto(CompanyAccount account, CompanyDto company, AuthorizedPersonelDto authorizedPerson) {
        if (account == null || company == null) return null;

        CompanyAccountDetailsDto dto = new CompanyAccountDetailsDto(
                account.getAccountNumber(),
                account.getType(),
                account.getAvailableBalance(),
                BigDecimal.ZERO,
                account.getBalance(),
                authorizedPerson //  Dodato ovlašćeno lice
        );

        dto.setCompanyName(company.getName()); // Postavljamo pravi naziv firme
        return dto;
    }

    public AccountTypeDto toAccountTypeDto(Account account) {
        if (account == null) return null;
        AccountTypeDto dto = new AccountTypeDto();
        dto.setAccountNumber(account.getAccountNumber());
        if (account instanceof PersonalAccount) {
            dto.setSubtype("Personal");
        } else if (account instanceof CompanyAccount) {
            dto.setSubtype("Company");
        }
        return dto;
    }
}
