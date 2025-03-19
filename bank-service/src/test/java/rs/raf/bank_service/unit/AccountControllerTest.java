package rs.raf.bank_service.unit;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private ExchangeRateService exchangeRateService;

    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private AccountController accountController;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserClient userClient;

    @MockBean
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAccounts() throws Exception {
        ClientDto clientDto = new ClientDto();
        clientDto.setFirstName("Marko");
        clientDto.setLastName("Markovic");

        String token = "Bearer valid-token";

        AccountDto accountDto = new AccountDto();
        accountDto.setAccountNumber("123456789012345678");
        accountDto.setOwner(clientDto);

        List<AccountDto> accounts = List.of(accountDto);
        Page<AccountDto> page = new PageImpl<>(accounts);

        when(jwtTokenUtil.getUserRoleFromAuthHeader(token))
                .thenReturn("EMPLOYEE");

        when(accountService.getAccounts(anyString(), anyString(), anyString(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/account")
                        .header("Authorization", token)
                        .param("accountNumber", "123456789012345678")
                        .param("firstName", "Marko")
                        .param("lastName", "Markovic")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateBankAccount_Success() throws Exception {
        // Kreiramo objekat za novi bankovni račun
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        // Pretpostavljamo da je korisnik već autentifikovan sa tokenom
        String authorizationHeader = "Bearer token";

        // Pretpostavljamo da metoda createNewBankAccount uspešno kreira novi bankovni račun
        doNothing().when(accountService).createNewBankAccount(any(NewBankAccountDto.class), anyString());


        // Simuliramo poziv POST zahteva na /api/account sa neophodnim parametarima
        mockMvc.perform(post("/api/account")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newBankAccountDto)))
                .andExpect(status().isCreated()); // Očekujemo HTTP status 201 (Created)

        // Verifikujemo da je metoda createNewBankAccount pozvana sa odgovarajućim argumentima
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }


    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateBankAccount_ClientNotFound() {
        // Arrange
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        String errorMessage = "Cannot find client with id: 999";
        doThrow(new ClientNotFoundException(999L)).when(accountService)
                .createNewBankAccount(any(NewBankAccountDto.class), anyString());

        // Act
        ResponseEntity<?> response = accountController.createBankAccount(authorizationHeader, newBankAccountDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateBankAccount_InvalidCurrency() {

        // Arrange
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        String errorMessage = "Currency not found: INVALID";
        doThrow(new CurrencyNotFoundException("INVALID")).when(accountService)
                .createNewBankAccount(any(NewBankAccountDto.class), anyString());


        // Act
        ResponseEntity<?> response = accountController.createBankAccount(authorizationHeader, newBankAccountDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }

//    @Test
//    @WithMockUser(roles = "CLIENT")
//    void testGetMyAccounts_Success() {
//        ResponseEntity<?> response = accountController.getAccounts("Bearer token");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMyAccounts_BadRequest() throws Exception {
        String authHeader = "Bearer valid-token";
        Long clientId = 123L; // Simulirani clientId

        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader))
                .thenReturn("CLIENT");

        // Simulacija greške u jwtTokenUtil da vrati clientId iz Authorization header-a
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);

        // Mockovanje accountService da baci UserNotAClientException
        when(accountService.getMyAccounts(clientId)).thenThrow(new UserNotAClientException());

        // Pozivamo endpoint sa mockovanim auth headerom
        mockMvc.perform(get("/api/account")
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest()) // Očekujemo status 400
                .andExpect(jsonPath("$").value("User sending request is not a client."));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMyAccounts_Failure() throws Exception {
        String authHeader = "Bearer valid-token";
        Long clientId = 123L; // Simulirani clientId

        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader))
                .thenReturn("CLIENT");

        // Mockovanje jwtTokenUtil da vrati clientId iz Authorization header-a
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);

        // Mockovanje accountService da baci RuntimeException kada se pozove getMyAccounts
        when(accountService.getMyAccounts(clientId)).thenThrow(new RuntimeException("Unexpected error occurred."));

        // Pozivamo endpoint i proveravamo da li se vraća 500 Internal Server Error
        mockMvc.perform(get("/api/account")
                        .header("Authorization", authHeader))
                .andExpect(status().isInternalServerError()) // Očekujemo status 500
                .andExpect(jsonPath("$").value("Unexpected error occurred.")); //
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetAccountDetails_BadRequest() throws Exception {
        String authHeader = "Bearer valid-token";

        String accountNumber = "1";

        // Simulacija UserNotAClientException u accountService
        when(accountService.getAccountDetails(anyLong(), eq(accountNumber)))
                .thenThrow(new UserNotAClientException());

        // Pozivamo endpoint sa mockovanim auth headerom
        mockMvc.perform(get("/api/account/details/{accountNumber}", accountNumber)
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest()) // Očekujemo status 400 (Bad Request)
                .andExpect(jsonPath("$").value("User sending request is not a client.")); // Očekujemo telo sa porukom greške

        verify(accountService).getAccountDetails(anyLong(), eq(accountNumber));
    }


    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetAccountDetails_Failure() throws Exception {
        String authHeader = "Bearer valid-token";
        String accountNumber = "1";

        // Simulacija RuntimeException
        when(accountService.getAccountDetails(anyLong(), eq(accountNumber)))
                .thenThrow(new RuntimeException("Account details retrieval failed"));

        // Pozivamo endpoint sa mockovanim auth headerom
        mockMvc.perform(get("/api/account/details/{accountNumber}", accountNumber)
                        .header("Authorization", authHeader))
                .andExpect(status().isInternalServerError()) // Očekujemo status 500 (Internal Server Error)
                .andExpect(jsonPath("$").value("Account details retrieval failed")); // Očekujemo telo sa porukom greške

        verify(accountService).getAccountDetails(anyLong(), eq(accountNumber));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetAccountsForClient_Success() throws Exception {
        Long clientId = 1L;
        String accountNumber = "123456789012345678";

        // Dummy account
        AccountDto accountDto = new AccountDto();
        accountDto.setAccountNumber(accountNumber);

        Page<AccountDto> page = new PageImpl<>(List.of(accountDto));

        when(accountService.getAccountsForClient(eq(accountNumber), eq(clientId), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/account/{clientId}", clientId)
                        .param("accountNumber", accountNumber)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").value(accountNumber));

        verify(accountService).getAccountsForClient(eq(accountNumber), eq(clientId), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testChangeAccountName_Success() throws Exception {
        String accountNumber = "123456789012345678";
        String authHeader = "Bearer valid-token";

        ChangeAccountNameDto dto = new ChangeAccountNameDto();
        dto.setNewName("My Savings");

        // Mock da metoda prolazi bez exceptiona
        doNothing().when(accountService).changeAccountName(eq(accountNumber), eq(dto.getNewName()), eq(authHeader));

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Account name updated successfully"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testChangeAccountName_DuplicateName() throws Exception {
        String accountNumber = "123456789012345678";
        String authHeader = "Bearer valid-token";

        ChangeAccountNameDto dto = new ChangeAccountNameDto();
        dto.setNewName("Duplicate Name");

        doThrow(new DuplicateAccountNameException("Name already exists")).when(accountService)
                .changeAccountName(eq(accountNumber), eq(dto.getNewName()), eq(authHeader));

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Name already exists"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testChangeAccountName_AccountNotFound() throws Exception {
        String accountNumber = "123456789012345678";
        String authHeader = "Bearer valid-token";

        ChangeAccountNameDto dto = new ChangeAccountNameDto();
        dto.setNewName("New Name");

        doThrow(new AccountNotFoundException()).when(accountService)
                .changeAccountName(eq(accountNumber), eq(dto.getNewName()), eq(authHeader));

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Account not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testChangeAccountLimit_Success() throws Exception {
        Long accountId = 1L;

        doNothing().when(accountService).changeAccountLimit(accountId);

        mockMvc.perform(put("/api/account/{id}/change-limit", accountId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Account limit updated successfully"));

        verify(accountService, times(1)).changeAccountLimit(accountId);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testRequestChangeAccountLimit_Success() throws Exception {
        String accountNumber = "123456789";
        String authHeader = "Bearer valid-token";

        ChangeAccountLimitDto dto = new ChangeAccountLimitDto();
        dto.setNewLimit(new BigDecimal("1000.0"));

        doNothing().when(accountService).requestAccountLimitChange(eq(accountNumber), eq(BigDecimal.valueOf(1000.0)), eq(authHeader));

        mockMvc.perform(put("/api/account/{accountNumber}/request-change-limit", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Limit change request saved. Awaiting approval."));

        verify(accountService, times(1)).requestAccountLimitChange(accountNumber, dto.getNewLimit(), authHeader);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testRequestChangeAccountLimit_InvalidLimit() throws Exception {
        String authHeader = "Bearer valid-token";

        ChangeAccountLimitDto request = new ChangeAccountLimitDto();
        request.setNewLimit(BigDecimal.valueOf(-100)); // Negativan limit da baci validation error

        mockMvc.perform(put("/api/account/123456789/request-change-limit")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }


}
