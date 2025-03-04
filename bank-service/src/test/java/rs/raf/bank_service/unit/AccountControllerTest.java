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
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.dto.ChangeAccountNameDto;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.exceptions.InvalidLimitException;
import rs.raf.bank_service.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private Long validAccountId;
    private String validNewName;
    private BigDecimal validNewLimit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build(); // Ručna inicijalizacija

        validAccountId = 1L;
        validNewName = "Updated Account";
        validNewLimit = new BigDecimal("5000");
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

    //  Promena imena - račun nije pronađen
    @Test
    void changeAccountName_AccountNotFound() throws Exception {
        doThrow(new AccountNotFoundException("Account not found"))
                .when(accountService).changeAccountName(validAccountId, validNewName);

        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/accounts/{id}/change-name", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).changeAccountName(validAccountId, validNewName);
    }

    //  Promena imena - duplikat imena
    @Test
    void changeAccountName_DuplicateName() throws Exception {
        doThrow(new DuplicateAccountNameException("Account name already in use"))
                .when(accountService).changeAccountName(validAccountId, validNewName);

        ChangeAccountNameDto request = new ChangeAccountNameDto(validNewName);

        mockMvc.perform(put("/api/accounts/{id}/change-name", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(accountService, times(1)).changeAccountName(validAccountId, validNewName);
    }

    //  Uspešna promena limita
    @Test
    void changeAccountLimit_Success() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit);

        mockMvc.perform(put("/api/accounts/{id}/change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account limit updated successfully"));

        verify(accountService, times(1)).changeAccountLimit(validAccountId, validNewLimit);
    }


    //  Promena limita - račun nije pronađen
    @Test
    void changeAccountLimit_AccountNotFound() throws Exception {
        doThrow(new AccountNotFoundException("Account not found"))
                .when(accountService).changeAccountLimit(validAccountId, validNewLimit);

        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit);

        mockMvc.perform(put("/api/accounts/{id}/change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).changeAccountLimit(validAccountId, validNewLimit);
    }

    //  Promena limita - negativan ili nula
    @Test
    void changeAccountLimit_InvalidValues() throws Exception {
        mockMvc.perform(put("/api/accounts/1/change-limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLimit\": 0}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.ZERO);

        doThrow(new IllegalArgumentException("Limit must be greater than zero"))
                .when(accountService).changeAccountLimit(validAccountId, BigDecimal.ZERO);

        mockMvc.perform(put("/api/accounts/{id}/change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Očekujemo HTTP 400

//       verify(accountService, times(1)).changeAccountLimit(validAccountId, BigDecimal.ZERO);
        verify(accountService, never()).changeAccountLimit(anyLong(), any());
    }


}
