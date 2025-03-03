package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.domain.dto.UserDto;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.AccountStatus;
import rs.raf.bank_service.domain.enums.AccountType;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Service
@AllArgsConstructor
public class AccountService {
    @Autowired
    UserService userService;
    private final CurrencyRepository currencyRepository;
    private final AccountRepository accountRepository;

    public void createNewBankAccount(NewBankAccountDto newBankAccountDto, String authorizationHeader) {
        Long userId = newBankAccountDto.getClientId();
        UserDto userDto = userService.getUserById(userId, authorizationHeader);
        if (userDto == null)
            throw new ClientNotFoundException(userId);
        Account newAccount;
        if (newBankAccountDto.getAccountType().equals(AccountOwnerType.COMPANY.toString())) {
            newAccount = new CompanyAccount();
            ((CompanyAccount) newAccount).setCompanyId(newBankAccountDto.getCompanyId());
        } else
            newAccount = new PersonalAccount();

        newAccount.setClientId(newBankAccountDto.getClientId());
        newAccount.setCreatedByEmployeeId(newBankAccountDto.getEmployeeId());
        newAccount.setCreationDate(LocalDate.ofEpochDay(Instant.now().getEpochSecond()));
        System.out.println(newBankAccountDto.getCurrency());
        Currency currCurrency = currencyRepository.findByCode(newBankAccountDto.getCurrency()).orElseThrow(() -> new CurrencyNotFoundException(newBankAccountDto.getCurrency()));
        newAccount.setCurrency(currCurrency);
        newAccount.setStatus(AccountStatus.valueOf(newBankAccountDto.getIsActive()));
        newAccount.setType(AccountType.valueOf(newBankAccountDto.getAccountType()));
        newAccount.setAccountOwnerType(AccountOwnerType.valueOf(newBankAccountDto.getAccountOwnerType()));
        newAccount.setBalance(newBankAccountDto.getInitialBalance());
        newAccount.setAvailableBalance(newBankAccountDto.getInitialBalance());
        newAccount.setDailyLimit(newBankAccountDto.getDailyLimit());
        newAccount.setMonthlyLimit(newBankAccountDto.getMonthlyLimit());
        newAccount.setDailySpending(newBankAccountDto.getDailySpending());
        newAccount.setMonthlySpending(newBankAccountDto.getMonthlySpending());

        String random = String.format("%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
        String accountOwnerTypeNumber = "";
        switch (newBankAccountDto.getAccountOwnerType()) {
            case "PERSONAL" -> accountOwnerTypeNumber = "11";
            case "COMPANY" -> accountOwnerTypeNumber = "12";
            case "SAVINGS" -> accountOwnerTypeNumber = "13";
            case "RETIREMENT" -> accountOwnerTypeNumber = "14";
            case "YOUTH" -> accountOwnerTypeNumber = "15";
            case "STUDENT" -> accountOwnerTypeNumber = "16";
            case "UNEMPLOYED" -> accountOwnerTypeNumber = "17";
        }

        String accountNumber = "3330001" + random + accountOwnerTypeNumber;
        newAccount.setAccountNumber(accountNumber);


        accountRepository.save(newAccount);
    }
}
