package rs.raf.bank_service.unit;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.exceptions.ClientNotAccountOwnerException;
import rs.raf.bank_service.exceptions.UserNotAClientException;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.service.AccountService;

import java.util.Arrays;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;


    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test

    @WithMockUser(authorities = "admin")
    void testGetAccounts() throws Exception {
        ClientDto clientDto = new ClientDto();
        clientDto.setFirstName("Marko");
        clientDto.setLastName("Markovic");

        AccountDto accountDto = new AccountDto();
        accountDto.setAccountNumber("123456789012345678");
        accountDto.setOwner(clientDto);

        List<AccountDto> accounts = Arrays.asList(accountDto);
        Page<AccountDto> page = new PageImpl<>(accounts);

        Mockito.when(accountService.getAccounts(anyString(), anyString(), anyString(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/account")
                        .param("accountNumber", "123456789012345678")
                        .param("firstName", "Marko")
                        .param("lastName", "Markovic")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").value("123456789012345678"))
                .andExpect(jsonPath("$.content[0].owner.firstName").value("Marko"))
                .andExpect(jsonPath("$.content[0].owner.lastName").value("Markovic"));
    }

    @Test

    void testCreateBankAccount_Success() {
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        ResponseEntity<String> response = accountController.createBankAccount(authorizationHeader, newBankAccountDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }

    @Test

    void testCreateBankAccount_ClientNotFound() {
        // Arrange
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        String errorMessage = "Cannot find client with id: 999";
        doThrow(new ClientNotFoundException(999L)).when(accountService)
                .createNewBankAccount(any(NewBankAccountDto.class), anyString());

        // Act
        ResponseEntity<String> response = accountController.createBankAccount(authorizationHeader, newBankAccountDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }

    @Test
    void testCreateBankAccount_InvalidCurrency() {

        // Arrange
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        String errorMessage = "Cannot find currency with id: INVALID";
        doThrow(new CurrencyNotFoundException("INVALID")).when(accountService)
                .createNewBankAccount(any(NewBankAccountDto.class), anyString());


        // Act
        ResponseEntity<String> response = accountController.createBankAccount(authorizationHeader, newBankAccountDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }

    @Test
    void testGetMyAccounts_Success() {
        ResponseEntity<?> response = accountController.getMyAccounts("Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(accountService).getMyAccounts("Bearer token");
    }

    @Test
    void testGetMyAccounts_BadRequest() {
        doThrow(new UserNotAClientException()).when(accountService).getMyAccounts("Bearer token");

        ResponseEntity<?> response = accountController.getMyAccounts("Bearer token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User sending request is not a client.", response.getBody());
        verify(accountService).getMyAccounts("Bearer token");
    }

    @Test
    void testGetMyAccounts_Failure() {
        doThrow(new RuntimeException()).when(accountService).getMyAccounts("Bearer token");

        ResponseEntity<?> response = accountController.getMyAccounts("Bearer token");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(accountService).getMyAccounts("Bearer token");
    }

    @Test
    void testGetAccountDetails_Success() {
        ResponseEntity<?> response = accountController.getAccountDetails("Bearer token", "1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(accountService).getAccountDetails("Bearer token", "1");
    }

    @Test
    void testGetAccountDetails_BadRequest() {
        doThrow(new ClientNotAccountOwnerException()).when(accountService).getAccountDetails("Bearer token", "1");

        ResponseEntity<?> response = accountController.getAccountDetails("Bearer token", "1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Client sending request is not the account owner.", response.getBody());
        verify(accountService).getAccountDetails("Bearer token", "1");
    }

    @Test
    void testGetAccountDetails_Failure() {
        doThrow(new RuntimeException()).when(accountService).getAccountDetails("Bearer token", "1");

        ResponseEntity<?> response = accountController.getAccountDetails("Bearer token", "1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(accountService).getAccountDetails("Bearer token", "1");
    }
}
