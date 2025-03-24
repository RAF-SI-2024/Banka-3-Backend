package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.mapper.LoanMapper;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.LoanRepository;
import rs.raf.bank_service.service.LoanService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoanServiceTest {


    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LoanMapper loanMapper;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

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
//        TransactionStatus txStatus = mock(TransactionStatus.class);
//        mockStatic(TransactionAspectSupport.class)
//                .when(TransactionAspectSupport::currentTransactionStatus)
//                .thenReturn(txStatus);
    }

    //    @Test
//    void testLoanPayment() {
//        // Arrange
//        Loan loan = new Loan();
//        loan.setStatus(LoanStatus.APPROVED);
//        loan.setNextInstallmentDate(LocalDate.now());
//        loan.setNextInstallmentAmount(BigDecimal.valueOf(1000));
//        loan.setInstallments(List.of(new Installment()));
//        Account account = new CompanyAccount();
//        account.setBalance(BigDecimal.valueOf(500));
//        account.setClientId(99L);
//        loan.setAccount(account);
//
//        when(loanRepository.findByNextInstallmentDate(LocalDate.now())).thenReturn(List.of(loan));
//        when(accountRepository.findByIdForUpdate(any())).thenReturn(account);
//        when(userClient.getClientById(99L)).thenReturn(new ClientDto());
//
//        // Act
//        loanService.loanPayment();
//
//        // Assert
//        verify(scheduledExecutorService, times(1)).schedule(any(Runnable.class), eq(72L), eq(TimeUnit.HOURS));
//        verify(userClient).getClientById(99L);
//    }
    @Test
    void testGetClientLoans() {
        String authHeader = "Bearer validToken";
        Long clientId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Account account = new PersonalAccount();
        account.setClientId(clientId);
        List<Account> accounts = List.of(account);

        Loan loan = new Loan();
        Page<Loan> loanPage = new PageImpl<>(List.of(loan), pageable, 1);
        LoanShortDto loanShortDto = new LoanShortDto();

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(accountRepository.findByClientId(clientId)).thenReturn(accounts);
        when(loanRepository.findByAccountIn(accounts, pageable)).thenReturn(loanPage);
        when(loanMapper.toShortDto(loan)).thenReturn(loanShortDto);

        Page<LoanShortDto> result = loanService.getClientLoans(authHeader, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(loanRepository, times(1)).findByAccountIn(accounts, pageable);
    }

    @Test
    void testGetLoanById() {
        Long loanId = 1L;
        Loan loan = new Loan();
        LoanDto loanDto = new LoanDto();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanMapper.toDto(loan)).thenReturn(loanDto);

        Optional<LoanDto> result = loanService.getLoanById(loanId);

        assertTrue(result.isPresent());
        assertEquals(loanDto, result.get());
        verify(loanRepository, times(1)).findById(loanId);
    }
}
