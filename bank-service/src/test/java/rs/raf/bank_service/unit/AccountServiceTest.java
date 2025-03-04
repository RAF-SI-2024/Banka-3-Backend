package rs.raf.bank_service.unit;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ClientDto;

import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.domain.dto.UserDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Currency;

import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;

import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.UserService;

import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;


    @Mock
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAccounts_noFilters_returnsAllSorted() {
        // Pravimo tri dummy računa
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
        when(accountRepository.findAll(ArgumentMatchers.<Specification<Account>>any())).thenReturn(accountList);

        // Pravimo odgovarajuće ClientDto objekte
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

        // Kreiramo Pageable - prvi page sa dovoljno velikom veličinom da obuhvati sve
        Pageable pageable = PageRequest.of(0, 10);
        Page<AccountDto> result = accountService.getAccounts(null, null, null, pageable);

        // Očekujemo da su rezultati sortirani po prezimenu u rastućem redosledu:
        // "Jovic" < "Markovic" < "Zoric"
        List<AccountDto> dtos = result.getContent();
        assertEquals(3, dtos.size());
        assertEquals("456", dtos.get(0).getAccountNumber());
        assertEquals("123", dtos.get(1).getAccountNumber());
        assertEquals("789", dtos.get(2).getAccountNumber());
    }

    @Test
    public void testGetAccounts_filterByFirstName() {
        // Pravimo dva računa
        PersonalAccount account1 = new PersonalAccount();
        account1.setAccountNumber("123");
        account1.setClientId(1L);

        PersonalAccount account2 = new PersonalAccount();
        account2.setAccountNumber("456");
        account2.setClientId(2L);

        List<Account> accountList = List.of(account1, account2);
        when(accountRepository.findAll(ArgumentMatchers.<Specification<Account>>any())).thenReturn(accountList);

        // Postavljamo ClientDto objekte
        ClientDto client1 = new ClientDto();
        client1.setFirstName("Marko");
        client1.setLastName("Markovic");

        ClientDto client2 = new ClientDto();
        client2.setFirstName("Jovan");
        client2.setLastName("Jovic");

        when(userClient.getClientById(1L)).thenReturn(client1);
        when(userClient.getClientById(2L)).thenReturn(client2);

        Pageable pageable = PageRequest.of(0, 10);
        // Filtriramo po imenu koje sadrži "Mar"
        Page<AccountDto> result = accountService.getAccounts(null, "Mar", null, pageable);
        List<AccountDto> dtos = result.getContent();

        // Očekujemo da samo račun sa imenom "Marko" bude vraćen
        assertEquals(1, dtos.size());
        assertEquals("Marko", dtos.get(0).getOwner().getFirstName());
    }

    @Test
    public void testGetAccounts_filterByLastName() {
        // Pravimo dva računa
        PersonalAccount account1 = new PersonalAccount();
        account1.setAccountNumber("123");
        account1.setClientId(1L);

        PersonalAccount account2 = new PersonalAccount();
        account2.setAccountNumber("456");
        account2.setClientId(2L);

        List<Account> accountList = List.of(account1, account2);
        when(accountRepository.findAll(ArgumentMatchers.<Specification<Account>>any())).thenReturn(accountList);

        // Postavljamo ClientDto objekte
        ClientDto client1 = new ClientDto();
        client1.setFirstName("Marko");
        client1.setLastName("Markovic");

        ClientDto client2 = new ClientDto();
        client2.setFirstName("Jovan");
        client2.setLastName("Jovic");

        when(userClient.getClientById(1L)).thenReturn(client1);
        when(userClient.getClientById(2L)).thenReturn(client2);

        Pageable pageable = PageRequest.of(0, 10);
        // Filtriramo po prezimenu koje sadrži "vic"
        Page<AccountDto> result = accountService.getAccounts(null, null, "vic", pageable);
        List<AccountDto> dtos = result.getContent();

        // Očekujemo da su oba računa vraćena jer oba prezimena sadrže "vic"
        assertEquals(2, dtos.size());
    }

    @Test
    public void testGetAccounts_pagination() {
        // Kreiramo 5 računa
        List<Account> accountList = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            PersonalAccount account = new PersonalAccount();
            account.setAccountNumber(String.valueOf(i));
            account.setClientId(i);
            accountList.add(account);
        }
        when(accountRepository.findAll(ArgumentMatchers.<Specification<Account>>any())).thenReturn(accountList);

        // Kreiramo odgovarajuće ClientDto objekte sa različitim prezimenima radi
        // sortiranja
        for (long i = 1; i <= 5; i++) {
            ClientDto client = new ClientDto();
            client.setFirstName("First" + i);
            client.setLastName("Last" + i);
            when(userClient.getClientById(i)).thenReturn(client);
        }

        // Zatražimo page 1 (drugi page) sa veličinom stranice 2
        Pageable pageable = PageRequest.of(1, 2);
        Page<AccountDto> result = accountService.getAccounts(null, null, null, pageable);
        List<AccountDto> dtos = result.getContent();

        // Pošto se računi sortiraju po prezimenu (Last1, Last2, Last3, Last4, Last5),
        // drugi page treba da sadrži 3. i 4. račun (prema sortiranju)
        assertEquals(2, dtos.size());
        assertEquals("3", dtos.get(0).getAccountNumber());
        assertEquals("4", dtos.get(1).getAccountNumber());
    }

    @Test
    public void testCreateNewBankAccount_Success() {

        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(1L);
        newBankAccountDto.setAccountType("CURRENT");
        newBankAccountDto.setCurrency("EUR");
        newBankAccountDto.setIsActive("ACTIVE");
        newBankAccountDto.setAccountOwnerType("PERSONAL");

        UserDto userDto = new UserDto();
        userDto.setId(1L);

        Currency currency = new Currency();
        currency.setCode("EUR");

        when(userService.getUserById(1L, "Bearer token")).thenReturn(userDto);

        // Use eq() to match the exact "EUR" string.
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency));

        accountService.createNewBankAccount(newBankAccountDto, "Bearer token");

        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    public void testCreateNewBankAccount_ClientNotFound() {
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(999L);

        newBankAccountDto.setAccountType("PERSONAL");
        newBankAccountDto.setCurrency("USD");

        when(userService.getUserById(999L, "Bearer token")).thenReturn(null);


        Exception exception = assertThrows(ClientNotFoundException.class, () -> {
            accountService.createNewBankAccount(newBankAccountDto, "Bearer token");
        });
        // Adjusted expected message:
        assertEquals("Cannot find client with id: 999", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void testCreateNewBankAccount_InvalidCurrency() {

        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(1L);
        newBankAccountDto.setAccountType("PERSONAL");
        newBankAccountDto.setCurrency("INVALID");

        UserDto userDto = new UserDto();
        userDto.setId(1L);

        when(userService.getUserById(1L, "Bearer token")).thenReturn(userDto);

        // Mark this stubbing as lenient to avoid unnecessary stubbing exception.
        lenient().when(currencyRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        Exception exception = assertThrows(CurrencyNotFoundException.class, () -> {
            accountService.createNewBankAccount(newBankAccountDto, "Bearer token");
        });
        // Adjusted expected message:
        assertEquals("Cannot find currency with id: INVALID", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }
}

