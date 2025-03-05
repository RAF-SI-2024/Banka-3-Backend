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
    private String validVerificationCode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

        validAccountId = 1L;
        validNewName = "Updated Account";
        validNewLimit = new BigDecimal("5000");
        validVerificationCode = "123456";
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


    @Test
    void requestChangeAccountLimit_Success() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(validNewLimit, validVerificationCode);

        // ✅ MOCKUJEMO userClient da ne vraća null
        ClientDto mockClient = new ClientDto();
        mockClient.setEmail(validEmail);
        when(userClient.getClientById(validAccountId)).thenReturn(mockClient);

        // ✅ MOCKUJEMO da se zahtev uspešno sačuva u bazi
        when(changeLimitRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/accounts/{id}/request-change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Limit change request saved. Awaiting approval.")); // PROVERAVA TAČAN RESPONSE


    }

    // ❌ 2. Pokušaj promene limita sa nevalidnom vrednošću
    @Test
    void requestChangeAccountLimit_InvalidLimit() throws Exception {
        ChangeAccountLimitDto request = new ChangeAccountLimitDto(BigDecimal.ZERO, validVerificationCode);

        // Očekujemo da će servis baciti InvalidLimitException
        doThrow(new InvalidLimitException())
                .when(accountService).requestAccountLimitChange(validAccountId, validEmail, BigDecimal.ZERO);

        mockMvc.perform(put("/api/accounts/{id}/request-change-limit", validAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) ;// Očekujemo HTTP 400
                //.andExpect(content().string("Limit must be greater than zero")); // ✅ Postavljamo tačan sadržaj odgovora

    }

    @Test
    void requestChangeAccountLimit_AccountNotFound() throws Exception {
        // Arrange - Simuliramo da nalog ne postoji
        Long nonExistentAccountId = 999L;
        ChangeAccountLimitDto requestDto = new ChangeAccountLimitDto(BigDecimal.valueOf(5000), "123456");

        Mockito.doThrow(new AccountNotFoundException())
                .when(accountService)
                .changeAccountLimit(nonExistentAccountId);

        // Act - Pozivamo API metod
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

        mockMvc.perform(put("/api/accounts/{id}/change-limit", nonExistentAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound()) // Očekujemo 404 Not Found
                .andExpect(content().string("Account not found")); // Očekujemo ovu poruku

        // Assert - Proveravamo da se metoda pozvala jednom
        Mockito.verify(accountService, times(1)).changeAccountLimit(nonExistentAccountId);
    }

}










