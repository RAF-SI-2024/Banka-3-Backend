package rs.raf.bank_service.unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.domain.entity.CurrentAccount;
import rs.raf.bank_service.dto.ClientDto;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.ClientServiceClient;

import java.math.BigDecimal;
import java.util.Optional;

public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientServiceClient clientServiceClient; //  Mock za Feign Client

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    ///Uspešna promena imena naloga

    @Test
    void testChangeAccountName_Success() {
        Long accountId = 1L;
        String newName = "New Account Name";

        CurrentAccount account = new CurrentAccount();
        account.setAccountNumber("123456789");
        account.setClientId(5L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(5L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByNameAndClientId(newName, account.getClientId())).thenReturn(false);
        when(clientServiceClient.getClientById(account.getClientId())).thenReturn(clientDto);

        accountService.changeAccountName(accountId, newName);

        assertEquals(newName, account.getAccountNumber());
        verify(accountRepository, times(1)).save(account);
    }

    ///Pokušaj promene imena za nepostojeći nalog
    @Test
    void testChangeAccountName_AccountNotFound() {
        Long accountId = 1L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.changeAccountName(accountId, "New Name"));
    }

   /// Pokušaj promene u ime koje već postoji
    @Test
    void testChangeAccountName_DuplicateName() {
        Long accountId = 1L;
        String newName = "Duplicate Name";

        CurrentAccount account = new CurrentAccount();
        account.setAccountNumber("123456789");
        account.setClientId(5L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(5L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        when(accountRepository.existsByAccountNumberAndClientId(eq(newName), eq(account.getClientId()))).thenReturn(true);

        when(clientServiceClient.getClientById(account.getClientId())).thenReturn(clientDto);

        assertThrows(DuplicateAccountNameException.class, () -> accountService.changeAccountName(accountId, newName));
    }

    //Uspešna promena limita naloga
    @Test
    void testChangeAccountLimit_Success() {
        Long accountId = 1L;
        BigDecimal newLimit = new BigDecimal("5000");

        CurrentAccount account = new CurrentAccount();
        account.setAccountNumber("123456789");
        account.setDailyLimit(new BigDecimal("1000"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        accountService.changeAccountLimit(accountId, newLimit);

        assertEquals(newLimit, account.getDailyLimit());
        verify(accountRepository, times(1)).save(account);
    }

    //  Pokušaj promene imena na isto ime
    @Test
    void testChangeAccountName_SameName() {
        Long accountId = 1L;
        String existingName = "Same Name";

        CurrentAccount account = new CurrentAccount();
        account.setAccountNumber(existingName);
        account.setClientId(5L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByNameAndClientId(existingName, account.getClientId())).thenReturn(false);

        accountService.changeAccountName(accountId, existingName);

        assertEquals(existingName, account.getAccountNumber());
        verify(accountRepository, never()).save(account);
    }

    // Postavljanje limita na negativan broj ili nulu
    @Test
    void testChangeAccountLimit_InvalidValues() {
        Long accountId = 1L;
        CurrentAccount account = new CurrentAccount();
        account.setAccountNumber("123456789");
        account.setDailyLimit(new BigDecimal("1000"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> accountService.changeAccountLimit(accountId, new BigDecimal("-100")));
        assertThrows(IllegalArgumentException.class, () -> accountService.changeAccountLimit(accountId, new BigDecimal("0")));
    }
}
