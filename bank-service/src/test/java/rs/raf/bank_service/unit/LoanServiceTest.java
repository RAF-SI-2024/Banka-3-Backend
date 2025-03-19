package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.LoanRepository;
import rs.raf.bank_service.service.LoanService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @InjectMocks
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // MOCK za TransactionAspectSupport
        TransactionStatus txStatus = mock(TransactionStatus.class);
        mockStatic(TransactionAspectSupport.class)
                .when(TransactionAspectSupport::currentTransactionStatus)
                .thenReturn(txStatus);
    }

    @Test
    void testLoanPayment() {
        // Arrange
        Loan loan = new Loan();
        loan.setStatus(LoanStatus.APPROVED);
        loan.setNextInstallmentDate(LocalDate.now());
        loan.setNextInstallmentAmount(BigDecimal.valueOf(1000));
        loan.setInstallments(List.of(new Installment()));
        Account account = new CompanyAccount();
        account.setBalance(BigDecimal.valueOf(500));
        account.setClientId(99L);
        loan.setAccount(account);

        when(loanRepository.findByNextInstallmentDate(LocalDate.now())).thenReturn(List.of(loan));
        when(accountRepository.findByIdForUpdate(any())).thenReturn(account);
        when(userClient.getClientById(99L)).thenReturn(new ClientDto(99L, "John", "Doe", "john.doe@mail.com"));

        // Act
        loanService.loanPayment();

        // Assert
        verify(scheduledExecutorService, times(1)).schedule(any(Runnable.class), eq(72L), eq(TimeUnit.HOURS));
        verify(userClient).getClientById(99L);
    }
}
