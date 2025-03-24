package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.enums.VerificationStatus;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserClient userClient;

    @Mock
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    @InjectMocks
    private AccountController accountController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String authHeader = "Bearer token";
    private final String accountNumber = "123456789";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
    }

    @Test
    void getAccounts_ClientRole_ReturnsMyAccounts() throws Exception {
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);
        when(accountService.getMyAccounts(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/account").header("Authorization", authHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getAccounts_EmployeeRole_ReturnsPagedAccounts() throws Exception {
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("EMPLOYEE");
        Page<AccountDto> page = new PageImpl<>(List.of());
        when(accountService.getAccounts(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/account")
                        .header("Authorization", authHeader)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAccounts_Unauthorized_ThrowsException() throws Exception {
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/api/account").header("Authorization", authHeader))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAccountsForClient_ReturnsAccounts() throws Exception {
        Page<AccountDto> page = new PageImpl<>(List.of());
        when(accountService.getAccountsForClient(any(), anyLong(), any())).thenReturn(page);

        mockMvc.perform(get("/api/account/1").param("accountNumber", "123"))
                .andExpect(status().isOk());
    }

    @Test
    void getAccountsForClient_ClientNotFound() throws Exception {
        when(accountService.getAccountsForClient(any(), anyLong(), any()))
                .thenThrow(new ClientNotFoundException(1L));

        mockMvc.perform(get("/api/account/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBankAccount_Success() throws Exception {
        NewBankAccountDto dto = new NewBankAccountDto();
        dto.setClientId(1L);
        dto.setAccountType("PERSONAL");
        dto.setAccountOwnerType("PERSONAL");
        dto.setCurrency("RSD");
        dto.setInitialBalance(BigDecimal.TEN);
        dto.setDailyLimit(BigDecimal.TEN);
        dto.setMonthlyLimit(BigDecimal.TEN);
        dto.setDailySpending(BigDecimal.ONE);
        dto.setMonthlySpending(BigDecimal.ONE);
        dto.setIsActive("ACTIVE");

        when(accountService.createNewBankAccount(any(), eq(authHeader))).thenReturn(new AccountDto());

        mockMvc.perform(post("/api/account")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void changeAccountName_Success() throws Exception {
        ChangeAccountNameDto request = new ChangeAccountNameDto("New Name");

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account name updated successfully"));
    }

    @Test
    void changeAccountName_AccountNotFound() throws Exception {
        ChangeAccountNameDto request = new ChangeAccountNameDto("New Name");

        doThrow(new AccountNotFoundException()).when(accountService)
                .changeAccountName(eq(accountNumber), eq("New Name"), eq(authHeader));

        mockMvc.perform(put("/api/account/{accountNumber}/change-name", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));
    }

    @Test
    void requestChangeAccountLimit_Success() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.valueOf(1000));

        mockMvc.perform(put("/api/account/{accountNumber}/request-change-limit", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Limit change request saved. Awaiting approval."));
    }

    @Test
    void requestChangeAccountLimit_InvalidLimit() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.valueOf(0));

        doThrow(new InvalidLimitException()).when(accountService)
                .requestAccountLimitChange(eq(accountNumber), eq(BigDecimal.ZERO), eq(authHeader));

        mockMvc.perform(put("/api/account/{accountNumber}/request-change-limit", accountNumber)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeAccountLimit_Success() throws Exception {
        mockMvc.perform(put("/api/account/{id}/change-limit", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Account limit updated successfully"));
    }

    @Test
    void changeAccountLimit_RequestNotFound() throws Exception {
        doThrow(new IllegalStateException("No pending limit change request found"))
                .when(accountService).changeAccountLimit(1L);

        mockMvc.perform(put("/api/account/{id}/change-limit", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No pending limit change request found"));
    }

    @Test
    void setAuthorizedPerson_Success() throws Exception {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);

        mockMvc.perform(put("/api/account/{id}/set-authorized-person", 1L)
                        .header("Authorization", authHeader)
                        .param("authorizedPersonId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authorized person successfully assigned to account."));
    }

    @Test
    void setAuthorizedPerson_AccountNotFound() throws Exception {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);
        doThrow(new AccountNotFoundException()).when(accountService)
                .setAuthorizedPerson(anyLong(), anyLong(), anyLong());

        mockMvc.perform(put("/api/account/{id}/set-authorized-person", 1L)
                        .header("Authorization", authHeader)
                        .param("authorizedPersonId", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccountDetails_Success() throws Exception {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");
        when(accountService.getAccountDetails("CLIENT", 1L, accountNumber))
                .thenReturn(new AccountDetailsDto());

        mockMvc.perform(get("/api/account/details/{accountNumber}", accountNumber)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());
    }

    @Test
    void getAccountDetails_ClientNotOwner() throws Exception {
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(1L);
        when(jwtTokenUtil.getUserRoleFromAuthHeader(authHeader)).thenReturn("CLIENT");

        doThrow(new ClientNotAccountOwnerException()).when(accountService)
                .getAccountDetails("CLIENT", 1L, accountNumber);

        mockMvc.perform(get("/api/account/details/{accountNumber}", accountNumber)
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
    }
}
