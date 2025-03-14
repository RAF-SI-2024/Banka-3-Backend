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
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void testGetAccounts() {
        // Mock authentication role
        String authHeader = "Bearer mockToken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("ADMIN");

        AccountDto dto1 = new AccountDto();
        dto1.setAccountNumber("123456789");
        dto1.setClientId(1L);
        dto1.setCurrencyCode("RSD");

        AccountDto dto2 = new AccountDto();
        dto2.setAccountNumber("987654321");
        dto2.setClientId(2L);
        dto2.setCurrencyCode("RSD");

        // Wrap the list in a Page
        List<AccountDto> mockAccounts = Arrays.asList(dto1, dto2);
        Page<AccountDto> mockPage = new PageImpl<>(mockAccounts);

        when(accountService.getAccounts(any(), any(), any(), any())).thenReturn(mockPage);

        // Call controller method
        ResponseEntity<?> response = accountController.getAccounts(null, null, null, 0, 10, authHeader);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Page.class, response.getBody()); // Expecting a Page, not List
        assertEquals(2, ((Page<?>) response.getBody()).getContent().size());

        // Verify interactions
        verify(jwtTokenUtil).getUserRoleFromAuthHeader(authHeader);
        verify(accountService).getAccounts(any(), any(), any(), any());
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

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMyAccounts_Success() {
        // Mock authentication
        String authHeader = "Bearer mockToken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);

        AccountDto dto1 = new AccountDto();
        dto1.setAccountNumber("123456789");
        dto1.setCurrencyCode("RSD");

        AccountDto dto2 = new AccountDto();
        dto2.setAccountNumber("987654321");
        dto2.setCurrencyCode("RSD");

        // Mock service response
        List<AccountDto> mockAccounts = Arrays.asList(dto1, dto2);

        when(accountService.getMyAccounts(1L)).thenReturn(mockAccounts);

        // Call controller method
        ResponseEntity<?> response = accountController.getAccounts(null, null, null, 0, 10, authHeader);

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(List.class, response.getBody());
        assertEquals(2, ((List<?>) response.getBody()).size());

        // Verify interactions
        verify(jwtTokenUtil).getUserRoleFromAuthHeader(authHeader);
        verify(jwtTokenUtil).getUserIdFromAuthHeader(authHeader);
        verify(accountService).getMyAccounts(1L);
    }


    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMyAccounts_BadRequest() {
        // Mock authentication
        String authHeader = "Bearer mockToken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);

        // Simulate exception thrown by service
        when(accountService.getMyAccounts(1L)).thenThrow(new UserNotAClientException());

        // Call controller method
        ResponseEntity<?> response = accountController.getAccounts(null, null, null, 0, 10, authHeader);

        // Verify response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User sending request is not a client.", response.getBody());

        // Verify interactions
        verify(jwtTokenUtil).getUserRoleFromAuthHeader(authHeader);
        verify(jwtTokenUtil).getUserIdFromAuthHeader(authHeader);
        verify(accountService).getMyAccounts(1L);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMyAccounts_Failure() {
        // Mock authentication
        String authHeader = "Bearer mockToken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);

        // Simulate unexpected error
        when(accountService.getMyAccounts(1L)).thenThrow(new RuntimeException("Unexpected server error"));

        // Call controller method
        ResponseEntity<?> response = accountController.getAccounts(null, null, null, 0, 10, authHeader);

        // Verify response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unexpected error occurred.", response.getBody());

        // Verify interactions
        verify(jwtTokenUtil).getUserRoleFromAuthHeader(authHeader);
        verify(jwtTokenUtil).getUserIdFromAuthHeader(authHeader);
        verify(accountService).getMyAccounts(1L);
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

        when(accountService.getAccountDetails(anyLong(), eq(accountNumber)))
                .thenThrow(new RuntimeException("Account details retrieval failed"));

        mockMvc.perform(get("/api/account/details/{accountNumber}", accountNumber)
                        .header("Authorization", authHeader))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("Account details retrieval failed"));
    }


}
