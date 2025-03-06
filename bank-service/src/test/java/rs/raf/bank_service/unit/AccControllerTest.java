package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.AccController;
import rs.raf.bank_service.domain.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.domain.dto.ChangeAccountNameDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.entity.ChangeLimitRequest;
import rs.raf.bank_service.domain.enums.VerificationStatus;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.exceptions.InvalidLimitException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



public class AccControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    private AccountService accountService;

    @Mock
    private UserClient userClient;  // Ispravno mockovan UserClient

    @Mock
    AccountRepository accountRepository;

    @Mock
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    private String validEmail;


    @InjectMocks
    private AccController accountController;

    private Long validAccountId;
    private String validNewName;
    private BigDecimal validNewLimit;


    private final Long validRequestId = 1L;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

        validAccountId = 1L;
        validNewName = "Updated Account";
        validNewLimit = new BigDecimal("5000");
        validEmail = "client@example.com";
    }

    //  Uspešna promena imena
    @Test
    void changeAccountName_Success() throws Exception {
        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/accounts/{id}/change-name", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account name updated successfully"));

        verify(accountService, times(1)).changeAccountName(validAccountId, validNewName);
    }

    //  FIX: Promena imena - račun nije pronađen
    @Test
    void changeAccountName_AccountNotFound() throws Exception {
        doThrow(new AccNotFoundException("Account not found"))
                .when(accountService).changeAccountName(validAccountId, validNewName);

        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/accounts/{id}/change-name", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());  // Trebalo bi da vraća 404

        verify(accountService, times(1)).changeAccountName(validAccountId, validNewName);
    }

    // Promena imena - duplikat imena
    @Test
    void changeAccountName_DuplicateName() throws Exception {
        doThrow(new DuplicateAccountNameException("Account name already in use"))
                .when(accountService).changeAccountName(validAccountId, validNewName);

        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/accounts/{id}/change-name", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account name already in use"));


        verify(accountService, times(1)).changeAccountName(validAccountId, validNewName);
    }


    // ✅ 1. Uspešno kreiranje zahteva za promenu limita
    @Test
    void requestChangeAccountLimit_Success() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit);

        mockMvc.perform(put("/api/accounts/{id}/request-change-limit", validRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Limit change request saved. Awaiting approval."));
    }

    // ❌ 2. Pokušaj promene limita sa nevalidnom vrednošću
    @Test
    void requestChangeAccountLimit_InvalidLimit() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.ZERO);

        String authHeader = "Bearer valid.jwt.token"; // Simulacija validnog JWT tokena

        doThrow(new InvalidLimitException())
                .when(accountService)
                .requestAccountLimitChange(validRequestId, validEmail, BigDecimal.ZERO, authHeader);

        mockMvc.perform(put("/api/accounts/{id}/request-change-limit", validRequestId)
                        .header("Authorization", authHeader)  // Dodajemo Authorization header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ❌ 3. Pokušaj promene limita kada zahtev ne postoji
    @Test
    void requestChangeAccountLimit_RequestNotFound() throws Exception {
        Long invalidRequestId = 999L;
        ChangeAccountLimitDto requestDto = new ChangeAccountLimitDto(new BigDecimal("5000"));

        doThrow(new IllegalStateException("No pending limit change request found"))
                .when(accountService).changeAccountLimit(invalidRequestId);

        mockMvc.perform(put("/api/accounts/{id}/change-limit", invalidRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))  // ✅ Mora imati telo
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No pending limit change request found"));
    }

    // ❌ 4. Pokušaj promene limita kada nalog ne postoji
    @Test
    void requestChangeAccountLimit_AccountNotFound() throws Exception {
        Long invalidRequestId = 999L;
        ChangeAccountLimitDto requestDto = new ChangeAccountLimitDto(new BigDecimal("5000"));

        doThrow(new AccountNotFoundException())
                .when(accountService).changeAccountLimit(invalidRequestId); // ✅ Ispravljeno - prosleđujemo samo accountId

        mockMvc.perform(put("/api/accounts/{id}/change-limit", invalidRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))) // ✅ Dodato JSON telo
                .andExpect(status().isNotFound()) // Očekujemo 404
                .andExpect(content().string("Account not found"));
    }

}










