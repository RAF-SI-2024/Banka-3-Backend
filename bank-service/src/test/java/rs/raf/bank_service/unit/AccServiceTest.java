package rs.raf.bank_service.unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.service.AccountService;


import java.math.BigDecimal;
import java.util.Optional;

public class AccServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserClient userClient; // Novi naziv Feign Clienta

    @InjectMocks
    private AccountService accService; // Novi naziv servisa

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    /// Uspešna promena imena naloga
    @Test
    void testChangeAccountName_Success() {
        Long accountId = 1L;
        String newName = "New Account Name";

        PersonalAccount account = new PersonalAccount(); // Zameni odgovarajućom klasom
        account.setAccountNumber("123456789");
        account.setClientId(5L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(5L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByAccountNumberAndClientId(newName, account.getClientId())).thenReturn(false);
        when(userClient.getClientById(account.getClientId())).thenReturn(clientDto);

        accService.changeAccountName(accountId, newName);

        assertEquals(newName, account.getAccountNumber());
        verify(accountRepository, times(1)).save(account);
    }

    /// Pokušaj promene imena za nepostojeći nalog
    @Test
    void testChangeAccountName_AccountNotFound() {
        Long accountId = 1L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccNotFoundException.class, () -> accService.changeAccountName(accountId, "New Name"));
    }

    /// Pokušaj promene u ime koje već postoji
    @Test
    void testChangeAccountName_DuplicateName() {
        Long accountId = 1L;
        String newName = "Duplicate Name";

        PersonalAccount account = new PersonalAccount();
        account.setAccountNumber("123456789");
        account.setClientId(5L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(5L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByAccountNumberAndClientId(eq(newName), eq(account.getClientId()))).thenReturn(true);
        when(userClient.getClientById(account.getClientId())).thenReturn(clientDto);

        assertThrows(DuplicateAccountNameException.class, () -> accService.changeAccountName(accountId, newName));
    }


    @Test
    void testChangeAccountLimit_Success() {
        Long accountId = 1L;
        BigDecimal newLimit = new BigDecimal("5000");
        String verificationCode = "123456"; // Validan kod

        PersonalAccount account = new PersonalAccount();
        account.setAccountNumber("123456789");
        account.setDailyLimit(new BigDecimal("1000"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userClient.isVerificationApproved(accountId, verificationCode)).thenReturn(true); // Simuliramo uspešnu verifikaciju

        accService.changeAccountLimit(accountId, newLimit, verificationCode);

        assertEquals(newLimit, account.getDailyLimit());
        verify(accountRepository, times(1)).save(account);
    }

    //  Pokušaj promene imena na isto ime
    @Test
    void testChangeAccountName_SameName() {
        Long accountId = 1L;
        String existingName = "Same Name";

        PersonalAccount account = new PersonalAccount();
        account.setAccountNumber(existingName);
        account.setClientId(5L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByAccountNumberAndClientId(existingName, account.getClientId())).thenReturn(false);

        accService.changeAccountName(accountId, existingName);

        assertEquals(existingName, account.getAccountNumber());
        verify(accountRepository, never()).save(account);
    }

    @Test
    void testChangeAccountLimit_InvalidValues() {
        Long accountId = 1L;
        String verificationCode = "123456"; // Validan kod, ali vrednost nevalidna
        BigDecimal newLimit = new BigDecimal("5000"); // Dodajemo definiciju validnog limita

        PersonalAccount account = new PersonalAccount();
        account.setAccountNumber("123456789");
        account.setDailyLimit(new BigDecimal("1000"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userClient.isVerificationApproved(accountId, verificationCode)).thenReturn(true); // Simuliramo uspešnu verifikaciju

        // Negativan limit treba da izazove izuzetak
        assertThrows(IllegalArgumentException.class, () -> accService.changeAccountLimit(accountId, new BigDecimal("-100"), verificationCode));
        assertThrows(IllegalArgumentException.class, () -> accService.changeAccountLimit(accountId, new BigDecimal("0"), verificationCode));

        // Ako verifikacija nije odobrena, baca se izuzetak pre promene limita
        when(userClient.isVerificationApproved(accountId, verificationCode)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> accService.changeAccountLimit(accountId, newLimit, verificationCode));
    }

}
