package rs.raf.bank_service.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.repository.CompanyAccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private CompanyAccountRepository companyAccountRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserClient userClient;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void shouldThrowExceptionIfClientNotAccountOwner() {
        Account acc = new PersonalAccount();
        acc.setAccountNumber("123");
        acc.setClientId(1L);

        ClientDto caller = new ClientDto();
        caller.setId(2L); // != acc.getClientId()

        when(accountRepository.findByAccountNumber("123")).thenReturn(Optional.of(acc));
        when(userClient.getClientById(2L)).thenReturn(caller);

        assertThrows(ClientNotAccountOwnerException.class, () ->
                accountService.getAccountDetails("CLIENT", 2L, "123")
        );
    }

    @Test
    void shouldReturnDetailsDtoForClientOwner() {
        Account acc = new PersonalAccount();
        acc.setAccountNumber("123");
        acc.setClientId(1L);
        acc.setAccountOwnerType(AccountOwnerType.STUDENT);

        ClientDto caller = new ClientDto();
        caller.setId(1L);
        caller.setFirstName("Pera");
        caller.setLastName("Peric");

        when(accountRepository.findByAccountNumber("123")).thenReturn(Optional.of(acc));
        when(userClient.getClientById(1L)).thenReturn(caller);

        AccountDetailsDto result = accountService.getAccountDetails("CLIENT", 1L, "123");

        assertEquals("Pera Peric", result.getAccountOwner());
    }

    @Test
    void shouldReturnDetailsDtoForAdminOrOtherRole() {
        Account acc = new CompanyAccount();
        acc.setAccountNumber("123");
        acc.setClientId(1L);
        acc.setAccountOwnerType(AccountOwnerType.STUDENT);

        ClientDto client = new ClientDto();
        client.setId(1L);
        client.setFirstName("Ana");
        client.setLastName("Anic");

        when(accountRepository.findByAccountNumber("123")).thenReturn(Optional.of(acc));
        when(userClient.getClientById(1L)).thenReturn(client);

        AccountDetailsDto result = accountService.getAccountDetails("EMPLOYEE", 5L, "123");

        assertEquals("Ana Anic", result.getAccountOwner());
    }

    @Test
    void shouldReturnCompanyDetailsWithAuthorizedPerson() {
        CompanyAccount acc = new CompanyAccount();
        acc.setAccountNumber("456");
        acc.setClientId(1L);
        acc.setAccountOwnerType(AccountOwnerType.COMPANY);
        acc.setCompanyId(100L);
        acc.setAuthorizedPersonId(200L);

        ClientDto client = new ClientDto();
        client.setId(1L);
        client.setFirstName("Mika");
        client.setLastName("Mikic");

        CompanyDto companyDto = new CompanyDto();
        companyDto.setName("Test d.o.o.");
        companyDto.setRegistrationNumber("123");
        companyDto.setTaxId("456");
        companyDto.setAddress("Adresa 1");

        AuthorizedPersonelDto authorizedDto = new AuthorizedPersonelDto(200L, "Nikola", "Nikolic", 100L);
        authorizedDto.setEmail("nikola@example.com");
        authorizedDto.setAddress("Ulica 1");
        authorizedDto.setPhoneNumber("0601234567");
        authorizedDto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        authorizedDto.setGender("MALE");


        when(accountRepository.findByAccountNumber("456")).thenReturn(Optional.of(acc));
        when(userClient.getClientById(1L)).thenReturn(client);
        when(userClient.getCompanyById(100L)).thenReturn(companyDto);
        when(userClient.getAuthorizedPersonnelById(200L)).thenReturn(authorizedDto);

        AccountDetailsDto result = accountService.getAccountDetails("EMPLOYEE", 1L, "456");

        assertTrue(result instanceof CompanyAccountDetailsDto);
        CompanyAccountDetailsDto dto = (CompanyAccountDetailsDto) result;
        assertEquals("Test d.o.o.", dto.getCompanyName());
        assertEquals("Mika Mikic", dto.getAccountOwner());
    }

    @Test
    void shouldReturnCompanyDetailsWithoutAuthorizedPerson() {
        CompanyAccount acc = new CompanyAccount();
        acc.setAccountNumber("456");
        acc.setClientId(1L);
        acc.setAccountOwnerType(AccountOwnerType.COMPANY);
        acc.setCompanyId(100L);
        acc.setAuthorizedPersonId(null); // Nema ovlašćenog lica

        ClientDto client = new ClientDto();
        client.setId(1L);
        client.setFirstName("Mika");
        client.setLastName("Mikic");

        CompanyDto companyDto = new CompanyDto();
        companyDto.setName("Test d.o.o.");
        companyDto.setRegistrationNumber("123");
        companyDto.setTaxId("456");
        companyDto.setAddress("Adresa 1");

        when(accountRepository.findByAccountNumber("456")).thenReturn(Optional.of(acc));
        when(userClient.getClientById(1L)).thenReturn(client);
        when(userClient.getCompanyById(100L)).thenReturn(companyDto);

        AccountDetailsDto result = accountService.getAccountDetails("EMPLOYEE", 1L, "456");

        assertTrue(result instanceof CompanyAccountDetailsDto);
        CompanyAccountDetailsDto dto = (CompanyAccountDetailsDto) result;
        assertNull(dto.getAuthorizedPerson());
    }

    @Test
    void shouldThrowExceptionIfAccountNotFound() {
        when(accountRepository.findByAccountNumber("999")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                accountService.getAccountDetails("CLIENT", 1L, "999")
        );
    }

    @Test
    void shouldThrowRuntimeExceptionOnFeignError() {
        when(accountRepository.findByAccountNumber("123")).thenReturn(Optional.of(new PersonalAccount()));
        when(userClient.getClientById(anyLong())).thenThrow(FeignException.class);

        assertThrows(RuntimeException.class, () ->
                accountService.getAccountDetails("CLIENT", 1L, "123")
        );
    }

    @Test
    void getAccounts_FilterByFirstAndLastName_Success() {
        String accountNumber = "123";
        String firstName = "john";
        String lastName = "doe";
        Pageable pageable = PageRequest.of(0, 10);

        ClientDto clientDto = new ClientDto(1L, "John", "Doe");

        PersonalAccount account = new PersonalAccount();
        account.setClientId(1L);
        account.setAccountNumber(accountNumber);

        //when(accountRepository.findAll(any())).thenReturn(List.of(account));
        when(accountRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(account));
        when(userClient.getClientById(1L)).thenReturn(clientDto);

        Page<AccountDto> result = accountService.getAccounts(accountNumber, firstName, lastName, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(accountNumber, result.getContent().get(0).getAccountNumber());
    }

    @Test
    void getAccounts_PaginationOnly_Success() {
        String accountNumber = "acc";
        Pageable pageable = PageRequest.of(0, 1);

        ClientDto clientDto1 = new ClientDto(1L, "Ana", "Popovic");
        ClientDto clientDto2 = new ClientDto(2L, "Ivan", "Zoric");

        PersonalAccount acc1 = new PersonalAccount();
        acc1.setAccountNumber("acc1");
        acc1.setClientId(1L);
        PersonalAccount acc2 = new PersonalAccount();
        acc2.setAccountNumber("acc2");
        acc2.setClientId(2L);

        when(accountRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(acc1, acc2));

        when(userClient.getClientById(1L)).thenReturn(clientDto1);
        when(userClient.getClientById(2L)).thenReturn(clientDto2);

        Page<AccountDto> result = accountService.getAccounts(accountNumber, null, null, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("acc1", result.getContent().get(0).getAccountNumber());
    }

    @Test
    void getAccountsForClient_Success() {
        String accountNumber = "acc";
        Long clientId = 10L;
        Pageable pageable = PageRequest.of(0, 5);

        PersonalAccount acc = new PersonalAccount();
        acc.setAccountNumber(accountNumber);
        acc.setClientId(clientId);

        ClientDto clientDto = new ClientDto(clientId, "Pera", "Peric");

        when(userClient.getClientById(clientId)).thenReturn(clientDto);
        when(accountRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(acc)));

        Page<AccountDto> result = accountService.getAccountsForClient(accountNumber, clientId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(accountNumber, result.getContent().get(0).getAccountNumber());
    }


    @Test
    void getAccounts_FilterByNonMatchingName_EmptyResult() {
        String accountNumber = "acc";
        String firstName = "marko";
        String lastName = "markovic";
        Pageable pageable = PageRequest.of(0, 10);

        PersonalAccount acc = new PersonalAccount();
        acc.setClientId(1L);
        acc.setAccountNumber(accountNumber);

        when(accountRepository.findAll((Specification<Account>) any(Specification.class)))
                .thenReturn(Arrays.asList(acc));

        when(userClient.getClientById(1L)).thenReturn(new ClientDto(1L, "Ana", "Popovic"));

        Page<AccountDto> result = accountService.getAccounts(accountNumber, firstName, lastName, pageable);

        assertEquals(0, result.getTotalElements());
    }



    @Test
    void changeAccountName_Success() {
        String accountNumber = "123";
        String newName = "New Name";
        String auth = "Bearer token";
        Long clientId = 1L;

        Account account = new PersonalAccount();
        account.setClientId(clientId);
        account.setName("Old Name");

        when(jwtTokenUtil.getUserIdFromAuthHeader(auth)).thenReturn(clientId);
        when(accountRepository.findByAccountNumberAndClientId(accountNumber, clientId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByNameAndClientId(newName, clientId)).thenReturn(false);

        accountService.changeAccountName(accountNumber, newName, auth);

        assertEquals(newName, account.getName());
        verify(accountRepository).save(account);
    }

    @Test
    void changeAccountName_DuplicateName() {
        String accountNumber = "123";
        String newName = "Duplicate";
        String auth = "Bearer token";
        Long clientId = 1L;

        Account account = new PersonalAccount();
        account.setClientId(clientId);

        when(jwtTokenUtil.getUserIdFromAuthHeader(auth)).thenReturn(clientId);
        when(accountRepository.findByAccountNumberAndClientId(accountNumber, clientId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByNameAndClientId(newName, clientId)).thenReturn(true);

        assertThrows(DuplicateAccountNameException.class, () -> {
            accountService.changeAccountName(accountNumber, newName, auth);
        });
    }

    @Test
    void requestAccountLimitChange_Success() throws JsonProcessingException {
        String accountNumber = "123";
        BigDecimal limit = BigDecimal.valueOf(1000);
        String auth = "Bearer token";
        Long clientId = 1L;

        Account account = new PersonalAccount();
        account.setAccountNumber(accountNumber);
        account.setClientId(clientId);
        account.setDailyLimit(BigDecimal.valueOf(500));

        when(jwtTokenUtil.getUserIdFromAuthHeader(auth)).thenReturn(clientId);
        when(accountRepository.findByAccountNumberAndClientId(accountNumber, clientId)).thenReturn(Optional.of(account));
        when(changeLimitRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        accountService.requestAccountLimitChange(accountNumber, limit, auth);

        verify(userClient).createVerificationRequest(any());
    }

    @Test
    void requestAccountLimitChange_InvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.requestAccountLimitChange("123", BigDecimal.ZERO, "auth");
        });
    }

    @Test
    void changeAccountLimit_Success() {
        Long requestId = 1L;
        ChangeLimitRequest request = new ChangeLimitRequest("123", BigDecimal.valueOf(3000));
        request.setId(requestId);

        Account account = new PersonalAccount();
        account.setAccountNumber("123");

        when(changeLimitRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(accountRepository.findByAccountNumber("123")).thenReturn(Optional.of(account));

        accountService.changeAccountLimit(requestId);

        assertEquals(BigDecimal.valueOf(3000), account.getDailyLimit());
        assertEquals(VerificationStatus.APPROVED, request.getStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void changeAccountLimit_RequestNotFound() {
        when(changeLimitRequestRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> {
            accountService.changeAccountLimit(1L);
        });
    }

    @Test
    void getMyAccounts_Success() {
        Long clientId = 1L;

        Account acc = new PersonalAccount();
        acc.setAccountNumber("123");
        acc.setAvailableBalance(BigDecimal.valueOf(100));
        List<Account> accountList = List.of(acc);

        when(userClient.getClientById(clientId)).thenReturn(new ClientDto(clientId, "John", "Doe"));
        when(accountRepository.findAllByClientId(clientId)).thenReturn(accountList);

        List<AccountDto> result = accountService.getMyAccounts(clientId);
        assertEquals(1, result.size());
        assertEquals("123", result.get(0).getAccountNumber());
    }

    @Test
    void getMyAccounts_ClientNotFound() {
        Long clientId = 1L;
        when(userClient.getClientById(clientId)).thenThrow(FeignException.NotFound.class);
        assertThrows(UserNotAClientException.class, () -> {
            accountService.getMyAccounts(clientId);
        });
    }

    @Test
    void createNewBankAccount_ThrowsClientNotFound() {
        NewBankAccountDto dto = new NewBankAccountDto();
        dto.setClientId(1L);
        dto.setCurrency("RSD");

        when(userClient.getClientById(dto.getClientId())).thenReturn(null);

        assertThrows(ClientNotFoundException.class, () -> {
            accountService.createNewBankAccount(dto, "auth");
        });
    }

    @Test
    void createNewBankAccount_ThrowsCurrencyNotFound() {
        NewBankAccountDto dto = new NewBankAccountDto();
        dto.setClientId(1L);
        dto.setCurrency("RSD");
        dto.setAccountType("PERSONAL");
        dto.setAccountOwnerType("PERSONAL");
        dto.setInitialBalance(BigDecimal.TEN);
        dto.setDailyLimit(BigDecimal.TEN);
        dto.setMonthlyLimit(BigDecimal.TEN);
        dto.setDailySpending(BigDecimal.ONE);
        dto.setMonthlySpending(BigDecimal.ONE);

        when(userClient.getClientById(dto.getClientId())).thenReturn(new ClientDto());
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.empty());

        assertThrows(CurrencyNotFoundException.class, () -> {
            accountService.createNewBankAccount(dto, "auth");
        });
    }




    //ispod su promenjeni testovi iz "AuthorizedPersonServiceTest", mislim da ne nepotrebna posebna klasa za to




    @Test
    void setAuthorizedPerson_AccountNotFound() {
        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
        });
    }

    @Test
    void setAuthorizedPerson_UnauthorizedUser() {
        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setCompanyId(5L);

        CompanyDto companyDto = new CompanyDto();
        companyDto.setMajorityOwner(new ClientDto(99L, "Not", "Owner")); // nije owner

        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.of(companyAccount));
        when(userClient.getCompanyById(5L)).thenReturn(companyDto);

        assertThrows(UnauthorizedException.class, () -> {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
        });
    }

    @Test
    void setAuthorizedPerson_InvalidAuthorizedPerson() {
        Long companyAccountId = 1L;
        Long authorizedPersonId = 2L;
        Long employeeId = 10L;

        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setCompanyId(5L);

        CompanyDto companyDto = new CompanyDto();
        companyDto.setMajorityOwner(new ClientDto(employeeId, "Marko", "Markovic")); // jeste vlasnik

        List<AuthorizedPersonelDto> personnelList = List.of(); // prazna lista

        when(accountRepository.findById(companyAccountId)).thenReturn(Optional.of(companyAccount));
        when(userClient.getCompanyById(5L)).thenReturn(companyDto);
        when(userClient.getAuthorizedPersonnelByCompany(5L)).thenReturn(personnelList);

        assertThrows(InvalidAuthorizedPersonException.class, () -> {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
        });
    }





    @Test
    void shouldCreateValidPersonalAccount() {
        NewBankAccountDto dto = new NewBankAccountDto();
        dto.setAccountType("CURRENT"); // validan AccountType
        dto.setAccountOwnerType("PERSONAL"); // personal suffix = "11"
        dto.setClientId(1L);
        dto.setCurrency("RSD");
        dto.setIsActive("ACTIVE");
        dto.setInitialBalance(BigDecimal.valueOf(1000));
        dto.setDailyLimit(BigDecimal.valueOf(100));
        dto.setMonthlyLimit(BigDecimal.valueOf(300));
        dto.setDailySpending(BigDecimal.ZERO);
        dto.setMonthlySpending(BigDecimal.ZERO);

        String header = "Bearer test.token";

        when(jwtTokenUtil.getUserIdFromAuthHeader(header)).thenReturn(10L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        when(userClient.getClientById(1L)).thenReturn(clientDto);

        Currency currency = new Currency();
        currency.setCode("RSD");
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(currency));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountDto result = accountService.createNewBankAccount(dto, header);

        // Assert
        Account saved = captor.getValue();
        assertTrue(saved instanceof PersonalAccount); // else grana
        assertEquals(AccountType.CURRENT, saved.getType());
        assertEquals(AccountOwnerType.PERSONAL, saved.getAccountOwnerType());
        assertEquals(currency, saved.getCurrency());
        assertEquals(BigDecimal.valueOf(1000), saved.getBalance());
        assertTrue(saved.getAccountNumber().startsWith("3330001"));
        assertTrue(saved.getAccountNumber().endsWith("11"));
    }


    @ParameterizedTest
    @ValueSource(strings = { "PERSONAL", "SAVINGS", "RETIREMENT", "YOUTH", "STUDENT", "UNEMPLOYED" })
    void shouldCreatePersonalAccountWithCorrectSuffixBasedOnOwnerType(String ownerType) {
        // Arrange
        NewBankAccountDto dto = new NewBankAccountDto();
        dto.setAccountType("CURRENT"); // valid AccountType enum
        dto.setAccountOwnerType(ownerType);
        dto.setClientId(1L);
        dto.setCurrency("RSD");
        dto.setIsActive("ACTIVE");
        dto.setInitialBalance(BigDecimal.valueOf(1000));
        dto.setDailyLimit(BigDecimal.valueOf(100));
        dto.setMonthlyLimit(BigDecimal.valueOf(300));
        dto.setDailySpending(BigDecimal.ZERO);
        dto.setMonthlySpending(BigDecimal.ZERO);

        String header = "Bearer test.token";

        when(jwtTokenUtil.getUserIdFromAuthHeader(header)).thenReturn(10L);

        ClientDto clientDto = new ClientDto();
        clientDto.setId(1L);
        when(userClient.getClientById(1L)).thenReturn(clientDto);

        Currency currency = new Currency();
        currency.setCode("RSD");
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(currency));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountDto result = accountService.createNewBankAccount(dto, header);

        // Assert
        Account saved = captor.getValue();
        assertTrue(saved instanceof PersonalAccount);

        assertEquals(AccountType.CURRENT, saved.getType());
        assertEquals(AccountOwnerType.valueOf(ownerType), saved.getAccountOwnerType());
        assertEquals(currency, saved.getCurrency());

        assertNotNull(saved.getAccountNumber());
        assertTrue(saved.getAccountNumber().startsWith("3330001"));

        // Očekivani sufiks na osnovu ownerType
        String expectedSuffix = switch (ownerType) {
            case "PERSONAL" -> "11";
            case "SAVINGS" -> "13";
            case "RETIREMENT" -> "14";
            case "YOUTH" -> "15";
            case "STUDENT" -> "16";
            case "UNEMPLOYED" -> "17";
            default -> throw new IllegalArgumentException("Nepodržan accountOwnerType u testu: " + ownerType);
        };

        assertTrue(saved.getAccountNumber().endsWith(expectedSuffix),
                "Expected suffix " + expectedSuffix + " for ownerType " + ownerType +
                        ", but got: " + saved.getAccountNumber());
    }

    @Test
    void shouldThrowInvalidAuthorizedPersonExceptionWhenPersonNotInCompanyList() {
        // Arrange
        Long accountId = 1L;
        Long authorizedPersonId = 222L;
        Long employeeId = 99L;

        CompanyAccount account = new CompanyAccount();
        account.setAccountNumber(String.valueOf(accountId));
        account.setCompanyId(100L);
        account.setAccountNumber("ACC-001");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CompanyDto companyDto = new CompanyDto();
        ClientDto owner = new ClientDto();
        owner.setId(employeeId);
        companyDto.setMajorityOwner(owner);
        when(userClient.getCompanyById(100L)).thenReturn(companyDto);

        // Vrati listu ovlašćenih lica, ali nijedno sa traženim ID-jem
        AuthorizedPersonelDto otherPerson = new AuthorizedPersonelDto(999L, "Marko", "Marković", 100L);
        when(userClient.getAuthorizedPersonnelByCompany(100L)).thenReturn(List.of(otherPerson));

        // Act & Assert
        assertThrows(InvalidAuthorizedPersonException.class, () ->
                accountService.setAuthorizedPerson(accountId, authorizedPersonId, employeeId)
        );

        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAllBankAccounts_Success() {
        CompanyAccount bankAccount1 = new CompanyAccount();
        bankAccount1.setAccountNumber("999");
        bankAccount1.setCompanyId(1L);

        CompanyAccount bankAccount2 = new CompanyAccount();
        bankAccount2.setAccountNumber("888");
        bankAccount2.setCompanyId(1L);

        Page<CompanyAccount> bankAccountPage = new PageImpl<>(List.of(bankAccount1, bankAccount2));
        when(companyAccountRepository.findByCompanyId(eq(1L), any(Pageable.class))).thenReturn(bankAccountPage);

        List<AccountDto> result = accountService.getAllBankAccounts();

        assertEquals(2, result.size());
        assertEquals("888", result.get(0).getAccountNumber());
        assertEquals("999", result.get(1).getAccountNumber());
    }
}

