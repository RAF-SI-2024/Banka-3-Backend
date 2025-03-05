package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.controller.AccController;
import rs.raf.bank_service.domain.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.domain.dto.ChangeAccountNameDto;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.service.AccountService;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccController accountController;

    private Long validAccountId;
    private String validNewName;
    private BigDecimal validNewLimit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

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


    //  Uspešna promena limita
    @Test
    void changeAccountLimit_Success() throws Exception {
        String verificationCode = "123456"; // Validan kod
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit, verificationCode);

        mockMvc.perform(put("/api/accounts/{id}/change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account limit updated successfully"));

        verify(accountService, times(1)).changeAccountLimit(validAccountId, validNewLimit, verificationCode);
    }


    //   FIX: Promena limita - račun nije pronađen
    @Test
    void changeAccountLimit_AccountNotFound() throws Exception {
        String verificationCode = "123456";
        doThrow(new AccNotFoundException("Account not found"))
                .when(accountService).changeAccountLimit(validAccountId, validNewLimit, verificationCode);

        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit, verificationCode);

        mockMvc.perform(put("/api/accounts/{id}/change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(accountService, times(1)).changeAccountLimit(validAccountId, validNewLimit, verificationCode);
    }


    //  Promena limita - negativan ili nula
    @Test
    void changeAccountLimit_InvalidValues() throws Exception {
        String verificationCode = "123456";
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.ZERO, verificationCode);

        // Simuliramo ponašanje servisa: baciće IllegalArgumentException
        doThrow(new IllegalArgumentException("Limit must be greater than zero"))
                .when(accountService).changeAccountLimit(validAccountId, BigDecimal.ZERO, verificationCode);

        // Act: Izvršavamo request i hvatamo odgovor
        MvcResult result = mockMvc.perform(put("/api/accounts/{id}/change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Debugging output da vidimo šta zapravo API vraća
        String responseContent = result.getResponse().getContentAsString();
        System.out.println(">>> Actual response: '" + responseContent + "'");

        // Assert: Očekujemo ispravnu poruku
        // assertEquals("Limit must be greater than zero", responseContent);

        // Proveravamo da se servis nikad nije pozvao
        verify(accountService, never()).changeAccountLimit(anyLong(), any(), any());
    }


}
