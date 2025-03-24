package rs.raf.bank_service.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.service.AccountService;
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
        accountService = new AccountService(
                currencyRepository,
                accountRepository,
                changeLimitRequestRepository,
                jwtTokenUtil,
                userClient,
                objectMapper
        );
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
}
