package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.domain.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.domain.dto.ChangeAccountNameDto;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.exceptions.InvalidLimitException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String validAccountNumber = "123456789123456789";  // Changed from accountId to accountNumber
    @Mock
    AccountRepository accountRepository;
    private MockMvc mockMvc;
    @Mock
    private AccountService accountService;
    @Mock
    private UserClient userClient;  // Mocked UserClient correctly
    @Mock
    private ChangeLimitRequestRepository changeLimitRequestRepository;
    private String validEmail;
    @InjectMocks
    private AccountController accountController;
    private String validNewName;
    private BigDecimal validNewLimit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

        validNewName = "Updated Account";
        validNewLimit = new BigDecimal("5000");
        validEmail = "client@example.com";
    }

    @Test
    void changeAccountName_Success() throws Exception {
        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);
        String authorizationHeader = "Bearer token";

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", validAccountNumber)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account name updated successfully"));

        verify(accountService, times(1)).changeAccountName(validAccountNumber, validNewName, authorizationHeader);
    }

    //  FIX: Name change - account not found
    @Test
    void changeAccountName_AccountNotFound() throws Exception {
        String authorizationHeader = "Bearer token";
        doThrow(new AccNotFoundException("Account not found"))
                .when(accountService).changeAccountName(validAccountNumber, validNewName, authorizationHeader);  // Changed accountId to accountNumber

        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", validAccountNumber)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())  // Should return 404
                .andExpect(content().string("Account not found"));

        verify(accountService, times(1)).changeAccountName(validAccountNumber, validNewName, authorizationHeader);
    }

    // Name change - duplicate name
    @Test
    void changeAccountName_DuplicateName() throws Exception {
        String authorizationHeader = "Bearer token";
        doThrow(new DuplicateAccountNameException("Account name already in use"))
                .when(accountService).changeAccountName(validAccountNumber, validNewName, authorizationHeader);  // Changed accountId to accountNumber

        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", validAccountNumber)
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account name already in use"));

        verify(accountService, times(1)).changeAccountName(validAccountNumber, validNewName, authorizationHeader);
    }

    // ✅ 1. Successful limit change request
    @Test
    void requestChangeAccountLimit_Success() throws Exception {
        String authorizationHeader = "Bearer token";
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit);

        mockMvc.perform(put("/api/account/{accountNumber}/request-change-limit", validAccountNumber)// Changed accountId to accountNumber
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Limit change request saved. Awaiting approval."));
    }

    // ❌ 2. Invalid limit change request
    @Test
    void requestChangeAccountLimit_InvalidLimit() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.ZERO);

        String authHeader = "Bearer valid.jwt.token"; // Simulated valid JWT token

        doThrow(new InvalidLimitException())
                .when(accountService)
                .requestAccountLimitChange(validAccountNumber, BigDecimal.ZERO, authHeader);  // Changed accountId to accountNumber

        mockMvc.perform(put("/api/account/{accountNumber}/request-change-limit", validAccountNumber)  // Changed accountId to accountNumber
                        .header("Authorization", authHeader)  // Add Authorization header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ❌ 3. Request not found for limit change
    @Test
    void requestChangeAccountLimit_RequestNotFound() throws Exception {
        Long invalidRequestNumber = 1L;
        ChangeAccountLimitDto requestDto = new ChangeAccountLimitDto(new BigDecimal("5000"));

        doThrow(new IllegalStateException("No pending limit change request found"))
                .when(accountService).changeAccountLimit(invalidRequestNumber);  // Changed accountId to accountNumber

        mockMvc.perform(put("/api/account/{accountNumber}/change-limit", invalidRequestNumber)  // Changed accountId to accountNumber
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))  // ✅ It must have a body
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No pending limit change request found"));
    }

    // ❌ 4. Account not found during limit change
    @Test
    void requestChangeAccountLimit_AccountNotFound() throws Exception {
        Long invalidRequestNumber = 1L;
        ChangeAccountLimitDto requestDto = new ChangeAccountLimitDto(new BigDecimal("5000"));

        doThrow(new AccountNotFoundException())
                .when(accountService).changeAccountLimit(invalidRequestNumber);  // Changed accountId to accountNumber

        mockMvc.perform(put("/api/account/{accountNumber}/change-limit", invalidRequestNumber)  // Changed accountId to accountNumber
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))  // ✅ Added JSON body
                .andExpect(status().isNotFound())  // Expecting 404
                .andExpect(content().string("Account not found"));
    }
}
