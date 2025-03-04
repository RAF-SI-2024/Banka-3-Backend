package rs.raf.bank_service.service;

import io.cucumber.core.logging.Logger;
import io.cucumber.core.logging.LoggerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CurrentAccount;
import rs.raf.bank_service.dto.ClientDto;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.exceptions.InvalidLimitException;
import rs.raf.bank_service.repository.AccountRepository;



import java.math.BigDecimal;


@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientServiceClient clientServiceClient; // Feign Client za user-service


    public void changeAccountName(Long accountId, String newName) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        System.out.println(">>> Account found: Current Name = " + account.getAccountNumber());

        // Ako je novo ime isto kao staro, nema potrebe za promenom
        if (account.getAccountNumber().equals(newName)) {
            System.out.println(">>> New name is the same as the current name. No changes made.");
            return;
        }

        // Proveravamo postoji li ime već u bazi
        boolean exists = accountRepository.existsByAccountNumberAndClientId(newName, account.getClientId());
        System.out.println(">>> Checking if account name '" + newName + "' already exists for client ID " + account.getClientId() + ": " + exists);

        if (exists) {
            System.out.println(">>> ERROR: Account name '" + newName + "' is already in use for client ID " + account.getClientId());
            throw new DuplicateAccountNameException("Account name already in use");
        }

        System.out.println(">>> Changing account name from '" + account.getAccountNumber() + "' to '" + newName + "'");
        account.setAccountNumber(newName);
        accountRepository.save(account);

        System.out.println(">>> SUCCESS: Account name changed to '" + newName + "'");
    }





    public void changeAccountLimit(Long accountId, BigDecimal newLimit) {
        if (newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        account.setDailyLimit(newLimit);
        accountRepository.save(account);


        //    Umesto direktne promene, pravimo 2FA verifikacioni zahtev

//        verificationRequestService.createRequest(account.getClientId(), "CHANGE_LIMIT", accountId, newLimit);
//        System.out.println(">>> 2FA request created for account ID: " + accountId + " with new limit: " + newLimit)
//        U approveRequest() bismo dodali stvarnu promenu limita
//        Kad se zahtev odobri, tada se tek limit menja.
    }
}
