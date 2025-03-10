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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.UserNotAClientException;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

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
    @WithMockUser(authorities = "employee")
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
    @WithMockUser(authorities = "employee")
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
    @WithMockUser(authorities = "employee")
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
    @WithMockUser(authorities = "employee")
    void testCreateBankAccount_InvalidCurrency() {

        // Arrange
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        String errorMessage = "Currency not found: INVALID";
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
    @WithMockUser(authorities = "employee")
    void testGetMyAccounts_Success() {
        ResponseEntity<?> response = accountController.getMyAccounts("Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithMockUser(authorities = "isAuthenticated()")
    void testGetMyAccounts_BadRequest() throws Exception {
        String authHeader = "Bearer valid-token";
        Long clientId = 123L; // Simulirani clientId

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
    @WithMockUser(authorities = "employee")
    void testGetMyAccounts_Failure() throws Exception {
        String authHeader = "Bearer valid-token";
        Long clientId = 123L; // Simulirani clientId

        // Mockovanje jwtTokenUtil da vrati clientId iz Authorization header-a
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);

        // Mockovanje accountService da baci RuntimeException kada se pozove getMyAccounts
        when(accountService.getMyAccounts(clientId)).thenThrow(new RuntimeException("Account list retrieval failed"));

        // Pozivamo endpoint i proveravamo da li se vraća 500 Internal Server Error
        mockMvc.perform(get("/api/account")
                        .header("Authorization", authHeader))
                .andExpect(status().isInternalServerError()) // Očekujemo status 500
                .andExpect(jsonPath("$").value("Account list retrieval failed")); //
    }

    @Test
    @WithMockUser(authorities = "employee")
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
    @WithMockUser(authorities = "employee")
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

}
