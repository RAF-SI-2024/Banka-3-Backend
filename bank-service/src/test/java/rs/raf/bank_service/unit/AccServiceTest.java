package rs.raf.bank_service.unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.ChangeLimitRequest;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.enums.VerificationStatus;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.Optional;

public class AccServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private AccountService accService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Uspešna promena imena naloga
//    @Test
//    void testChangeAccountName_Success() {
//        String accountNumber = "123456789123456789";
//        String newName = "New Account Name";
//
//        PersonalAccount account = new PersonalAccount();
//        account.setAccountNumber(accountNumber);
//        account.setClientId(5L);
//
//        ClientDto clientDto = new ClientDto();
//        clientDto.setId(5L);
//
//        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
//        when(accountRepository.existsByAccountNumberAndClientId(newName, account.getClientId())).thenReturn(false);
//        when(userClient.getClientById(account.getClientId())).thenReturn(clientDto);
//
//        accService.changeAccountName(accountNumber, newName, "");
//
//        assertEquals(newName, account.getAccountNumber());
//        verify(accountRepository, times(1)).save(account);
//    }

    // Pokušaj promene imena za nepostojeći nalog
    @Test
    void testChangeAccountName_AccountNotFound() {
        String accountNumber = "123456789123456789";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccNotFoundException.class, () -> accService.changeAccountName(accountNumber, "New Name", ""));
    }

    // Pokušaj promene u ime koje već postoji
//    @Test
//    void testChangeAccountName_DuplicateName() {
//        String accountNumber = "123456789123456789";
//        String newName = "Duplicate Name";
//
//        PersonalAccount account = new PersonalAccount();
//        account.setAccountNumber(accountNumber);
//        account.setClientId(5L);
//
//        ClientDto clientDto = new ClientDto();
//        clientDto.setId(5L);
//
//        when(jwtTokenUtil.getUserIdFromAuthHeader("")).thenReturn(account.getClientId());
//        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
//        when(accountRepository.existsByAccountNumberAndClientId(newName, account.getClientId())).thenReturn(true);
//        when(userClient.getClientById(account.getClientId())).thenReturn(clientDto);
//
//        assertThrows(DuplicateAccountNameException.class, () -> accService.changeAccountName(accountNumber, newName, ""));
//    }

    // Pokušaj promene imena na isto ime
//    @Test
//    void testChangeAccountName_SameName() {
//        String accountNumber = "Same Name";
//        String existingName = "Same Name";
//
//        PersonalAccount account = new PersonalAccount();
//        account.setAccountNumber(existingName);
//        account.setClientId(5L);
//
//        when(jwtTokenUtil.getUserIdFromAuthHeader("")).thenReturn(account.getClientId());
//        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
//        when(accountRepository.existsByAccountNumberAndClientId(existingName, account.getClientId())).thenReturn(false);
//
//        accService.changeAccountName(accountNumber, existingName, "");
//
//        assertEquals(existingName, account.getAccountNumber());
//        verify(accountRepository, never()).save(account);
//    }

    @Test
    void testChangeAccountLimit_Success() {
        Long requestId = 1L;
        BigDecimal newLimit = new BigDecimal("5000");

        ChangeLimitRequest changeRequest = new ChangeLimitRequest();
        changeRequest.setId(requestId);
        changeRequest.setAccountNumber("123456789");  // Changed to accountNumber
        changeRequest.setNewLimit(newLimit);
        changeRequest.setStatus(VerificationStatus.PENDING);

        Account account = new Account() {};
        account.setAccountNumber("123456789");
        account.setDailyLimit(new BigDecimal("1000"));

        when(changeLimitRequestRepository.findById(requestId))
                .thenReturn(Optional.of(changeRequest));
        when(accountRepository.findByAccountNumber(changeRequest.getAccountNumber()))
                .thenReturn(Optional.of(account));

        accService.changeAccountLimit(requestId);

        // ✅ Proveravamo da li je limit ažuriran
        assertEquals(newLimit, account.getDailyLimit());
        // ✅ Proveravamo da li je nalog sačuvan
        verify(accountRepository, times(1)).save(account);
        // ✅ Proveravamo da li je status zahteva promenjen u APPROVED
        assertEquals(VerificationStatus.APPROVED, changeRequest.getStatus());
        verify(changeLimitRequestRepository, times(1)).save(changeRequest);
    }

    @Test
    void testChangeAccountLimit_NoPendingRequest() {
        Long requestId = 1L;

        when(changeLimitRequestRepository.findById(requestId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> accService.changeAccountLimit(requestId));
    }

    @Test
    void testChangeAccountLimit_AccountNotFound() {
        Long requestId = 1L;
        BigDecimal newLimit = new BigDecimal("5000");

        ChangeLimitRequest changeRequest = new ChangeLimitRequest();
        changeRequest.setId(requestId);
        changeRequest.setAccountNumber("123456789");  // Changed to accountNumber
        changeRequest.setNewLimit(newLimit);
        changeRequest.setStatus(VerificationStatus.PENDING);

        when(changeLimitRequestRepository.findById(requestId))
                .thenReturn(Optional.of(changeRequest));
        when(accountRepository.findByAccountNumber(changeRequest.getAccountNumber()))
                .thenReturn(Optional.empty());

        assertThrows(AccNotFoundException.class, () -> accService.changeAccountLimit(requestId));
    }
}
