package rs.raf.bank_service.unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.domain.dto.UserDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    public void testCreateNewBankAccount_Success() {
        // Given
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(1L);
        newBankAccountDto.setAccountType("CURRENT");
        newBankAccountDto.setCurrency("EUR");
        newBankAccountDto.setIsActive("ACTIVE");
        newBankAccountDto.setAccountOwnerType("PERSONAL");

        UserDto userDto = new UserDto();
        userDto.setId(1L);

        Currency currency = new Currency();
        currency.setCode("EUR");

        when(userService.getUserById(1L, "Bearer token")).thenReturn(userDto);
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency));

        // When
        accountService.createNewBankAccount(newBankAccountDto, "Bearer token");

        // Then
        verify(accountRepository, times(1)).save(any(Account.class));
    }
    @Test
    public void testCreateNewBankAccount_ClientNotFound() {
        // Given
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(999L); // Nepostojeći klijent
        newBankAccountDto.setAccountType("PERSONAL");
        newBankAccountDto.setCurrency("USD");

        when(userService.getUserById(999L, "Bearer token")).thenReturn(null);

        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            accountService.createNewBankAccount(newBankAccountDto, "Bearer token");
        });

        assertEquals("Client not found with ID: 999", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }
    @Test
    public void testCreateNewBankAccount_InvalidCurrency() {
        // Given
        NewBankAccountDto newBankAccountDto = new NewBankAccountDto();
        newBankAccountDto.setClientId(1L);
        newBankAccountDto.setAccountType("PERSONAL");
        newBankAccountDto.setCurrency("INVALID");

        UserDto userDto = new UserDto();
        userDto.setId(1L);

        when(userService.getUserById(1L, "Bearer token")).thenReturn(userDto);
        when(currencyRepository.findByCode("INVALID")).thenReturn(java.util.Optional.empty());

        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            accountService.createNewBankAccount(newBankAccountDto, "Bearer token");
        });

        assertEquals("Invalid currency.", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

}