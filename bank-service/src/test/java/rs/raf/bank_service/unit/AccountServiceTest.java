package rs.raf.bank_service.unit;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.AccountDetailsDto;
import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.exceptions.ClientNotAccountOwnerException;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.UserNotAClientException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        // Ažurirani konstruktor sa dodatim ChangeLimitRequestRepository
        accountService = new AccountService(currencyRepository, accountRepository, changeLimitRequestRepository, jwtTokenUtil, userClient);
    }

    @Test
    public void testGetAccounts_noFilters_returnsAllSorted() {
        // Kreiranje testnih računa
        PersonalAccount account1 = new PersonalAccount();
        account1.setAccountNumber("123");
        account1.setClientId(1L);

        PersonalAccount account2 = new PersonalAccount();
        account2.setAccountNumber("456");
        account2.setClientId(2L);

        PersonalAccount account3 = new PersonalAccount();
        account3.setAccountNumber("789");
        account3.setClientId(3L);

        List<Account> accountList = List.of(account1, account2, account3);

        // OVAJ DEO MENJAMO:
        when(accountRepository.findAll(any(Specification.class))).thenReturn(accountList);

        // Mock podaci za klijente
        ClientDto client1 = new ClientDto();
        client1.setFirstName("Marko");
        client1.setLastName("Markovic");

        ClientDto client2 = new ClientDto();
        client2.setFirstName("Jovan");
        client2.setLastName("Jovic");

        ClientDto client3 = new ClientDto();
        client3.setFirstName("Zoran");
        client3.setLastName("Zoric");

        when(userClient.getClientById(1L)).thenReturn(client1);
        when(userClient.getClientById(2L)).thenReturn(client2);
        when(userClient.getClientById(3L)).thenReturn(client3);

        // Kreiranje Pageable objekta
        Pageable pageable = PageRequest.of(0, 10);

        // Pozivanje metode
        Page<AccountDto> result = accountService.getAccounts(null, null, null, pageable);

        // Provera rezultata
        assertEquals(3, result.getContent().size());
        assertEquals("456", result.getContent().get(0).getAccountNumber());
        assertEquals("123", result.getContent().get(1).getAccountNumber());
        assertEquals("789", result.getContent().get(2).getAccountNumber());
    }


    @Test
    public void testCreateNewBankAccount_Success() {
        // Kreiranje objekta DTO sa svim potrebnim podacima
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(1L);
        newBankAccountDto.setAccountType("CURRENT");
        newBankAccountDto.setCurrency("EUR");
        newBankAccountDto.setIsActive("ACTIVE");
        newBankAccountDto.setAccountOwnerType("PERSONAL");  // Dodali smo accountOwnerType
        newBankAccountDto.setEmployeeId(123L);  // Dodali smo employeeId
        newBankAccountDto.setInitialBalance(new BigDecimal("1000.00"));
        newBankAccountDto.setDailyLimit(new BigDecimal("500.00"));
        newBankAccountDto.setMonthlyLimit(new BigDecimal("10000.00"));
        newBankAccountDto.setDailySpending(new BigDecimal("100.00"));
        newBankAccountDto.setMonthlySpending(new BigDecimal("5000.00"));
        newBankAccountDto.setCreateCard(true);

        // Kreiranje odgovarajućih objekata
        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);

        Currency currency = new Currency();
        currency.setCode("EUR");

        // Mocking odgovora
        when(userClient.getClientById(1L)).thenReturn(clientDto);
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency));

        // Pozivanje metode za kreiranje novog bankovnog računa
        accountService.createNewBankAccount(newBankAccountDto, "Bearer token");

        // Verifikacija da je metoda save() pozvana
        verify(accountRepository, times(1)).save(any(Account.class));
    }


    @Test
    public void testCreateNewBankAccount_ClientNotFound() {
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(999L);

        when(userClient.getClientById(999L)).thenReturn(null);

        Exception exception = assertThrows(ClientNotFoundException.class, () -> {
            accountService.createNewBankAccount(newBankAccountDto, "Bearer token");
        });
        assertEquals("Cannot find client with id: 999", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void testCreateNewBankAccount_InvalidCurrency() {
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(1L);
        newBankAccountDto.setAccountType("PERSONAL");
        newBankAccountDto.setCurrency("INVALID");

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);

        when(userClient.getClientById(1L)).thenReturn(clientDto);
        when(currencyRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        Exception exception = assertThrows(CurrencyNotFoundException.class, () -> {
            accountService.createNewBankAccount(newBankAccountDto, "Bearer token");
        });
        assertEquals("Currency not found: INVALID", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void testGetAccountDetails_Success() {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        when(userClient.getClientById(1L)).thenReturn(clientDto);

        PersonalAccount account = new PersonalAccount();
        account.setAccountNumber("1");
        account.setClientId(clientDto.getId());
        account.setBalance(BigDecimal.TEN);
        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));

        AccountDetailsDto accountDetails = accountService.getAccountDetails(1L, "1");
        assertNotNull(accountDetails);
        assertEquals(account.getBalance(), accountDetails.getBalance());
    }

    @Test
    public void testGetAccountDetails_ClientNotAccountOwner() {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        when(userClient.getClientById(1L)).thenReturn(clientDto);

        PersonalAccount account = new PersonalAccount();
        account.setAccountNumber("1");
        account.setClientId(99L);  // Different client id
        account.setBalance(BigDecimal.TEN);
        when(accountRepository.findByAccountNumber("1")).thenReturn(Optional.of(account));

        Exception exception = assertThrows(ClientNotAccountOwnerException.class, () -> {
            accountService.getAccountDetails(1L, "1");
        });
        assertEquals("Client sending request is not the account owner.", exception.getMessage());
    }

    @Test
    public void testGetMyAccounts_Success() {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        when(userClient.getClientById(1L)).thenReturn(clientDto);

        List<Account> accountList = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            PersonalAccount account = new PersonalAccount();
            account.setAccountNumber(String.valueOf(i));
            account.setClientId(clientDto.getId());
            accountList.add(account);
        }
        when(accountRepository.findAllByClientId(clientDto.getId())).thenReturn(accountList);

        List<AccountDto> accounts = accountService.getMyAccounts(1L);
        assertNotNull(accounts);
        assertEquals(5, accounts.size());
    }

    @Test
    public void testGetMyAccounts_UserNotAClient() {
        // Kreirajte Request objekat sa validnim parametrom
        Request request = Request.create(
                Request.HttpMethod.GET, // Metoda
                "http://localhost:8080", // URL, ovo može biti bilo koji validan URL
                new HashMap<>(), // Parametri, ako ih imate
                null, // Body, ako ga nemate, stavite null
                new RequestTemplate() // RequestTemplate
        );

        // Mock-ujte FeignException sa validnim Request objektom
        when(userClient.getClientById(5L)).thenThrow(
                new FeignException.NotFound("User not found", request, null, null)
        );

        // Pozivanje metode koja bi trebala da baci UserNotAClientException
        UserNotAClientException exception = assertThrows(UserNotAClientException.class, () -> {
            accountService.getMyAccounts(5L);
        });

        // Proverite da li je greška ta koja se očekuje
        assertEquals("User sending request is not a client.", exception.getMessage());
    }

}
