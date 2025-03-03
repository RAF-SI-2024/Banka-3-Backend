package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.service.AccountService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
    void testCreateBankAccount_Failure() {
        // Arrange
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        String authorizationHeader = "Bearer token";

        String errorMessage = "Invalid input data";
        doThrow(new RuntimeException(errorMessage)).when(accountService).createNewBankAccount(any(NewBankAccountDto.class), anyString());

        // Act
        ResponseEntity<String> response = accountController.createBankAccount(authorizationHeader, newBankAccountDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(accountService).createNewBankAccount(newBankAccountDto, authorizationHeader);
    }
}