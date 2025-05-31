package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.domain.mapper.InstallmentMapper;
import rs.raf.bank_service.domain.mapper.LoanMapper;
import rs.raf.bank_service.exceptions.InsufficientFundsException;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.service.LoanService;
import rs.raf.bank_service.service.TransactionQueueService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LoanMapper loanMapper;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private UserClient userClient;
    @Mock private ScheduledExecutorService scheduledExecutorService;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private InstallmentMapper installmentMapper;
    @Mock private TransactionQueueService transactionQueueService;
    @Mock private LoanRequestRepository loanRequestRepository;

    @InjectMocks private LoanService loanService;

    private Loan loan;
    private LoanDto loanDto;
    private LoanShortDto loanShortDto;
    private Installment installment;
    private InstallmentDto installmentDto;
    private Account account;
    private Currency currency;

    @BeforeEach
    void init() {
        currency = new Currency("RSD", "Dinar", "RSD", "RS", "Dinar", true, "");
        account = new PersonalAccount();
        account.setAccountNumber("12345");
        account.setClientId(1L);
        account.setBalance(BigDecimal.valueOf(50000));
        account.setAvailableBalance(BigDecimal.valueOf(50000));

        loan = Loan.builder()
                .id(1L)
                .loanNumber("LN123")
                .type(LoanType.CASH)
                .amount(BigDecimal.valueOf(100000))
                .repaymentPeriod(12)
                .nominalInterestRate(BigDecimal.valueOf(5.5))
                .effectiveInterestRate(BigDecimal.valueOf(6.0))
                .startDate(LocalDate.now().minusMonths(1))
                .dueDate(LocalDate.now().plusMonths(11))
                .nextInstallmentAmount(BigDecimal.valueOf(10000))
                .nextInstallmentDate(LocalDate.now())
                .remainingDebt(BigDecimal.valueOf(90000))
                .currency(currency)
                .status(LoanStatus.APPROVED)
                .account(account)
                .installments(new ArrayList<>())
                .build();

        loanDto = LoanDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .type(loan.getType())
                .amount(loan.getAmount())
                .repaymentPeriod(loan.getRepaymentPeriod())
                .nominalInterestRate(loan.getNominalInterestRate())
                .effectiveInterestRate(loan.getEffectiveInterestRate())
                .startDate(loan.getStartDate())
                .dueDate(loan.getDueDate())
                .nextInstallmentAmount(loan.getNextInstallmentAmount())
                .nextInstallmentDate(loan.getNextInstallmentDate())
                .remainingDebt(loan.getRemainingDebt())
                .currencyCode(loan.getCurrency().getCode())
                .status(loan.getStatus())
                .build();

        loanShortDto = new LoanShortDto(loan.getId(), loan.getLoanNumber(), loan.getType(), loan.getAmount());

        installment = Installment.builder()
                .id(1L)
                .loan(loan)
                .amount(BigDecimal.valueOf(10000))
                .interestRate(BigDecimal.valueOf(5.5))
                .expectedDueDate(LocalDate.now().plusMonths(1))
                .actualDueDate(null)
                .installmentStatus(InstallmentStatus.UNPAID)
                .build();

        installmentDto = InstallmentDto.builder()
                .amount(installment.getAmount())
                .interestRate(installment.getInterestRate())
                .expectedDueDate(installment.getExpectedDueDate())
                .actualDueDate(installment.getActualDueDate())
                .installmentStatus(installment.getInstallmentStatus())
                .build();
    }

    @Test
    void testGetLoanById_Success() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanMapper.toDto(loan)).thenReturn(loanDto);

        Optional<LoanDto> result = loanService.getLoanById(1L);
        assertTrue(result.isPresent());
        assertEquals(loanDto.getLoanNumber(), result.get().getLoanNumber());
    }

    @Test
    void testGetLoanById_NotFound() {
        when(loanRepository.findById(2L)).thenReturn(Optional.empty());
        Optional<LoanDto> result = loanService.getLoanById(2L);
        assertFalse(result.isPresent());
    }

    @Test
    void testGetClientLoans_ReturnsPage() {
        String token = "Bearer token";
        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(1L);
        when(accountRepository.findByClientId(1L)).thenReturn(List.of(account));

        Page<Loan> loans = new PageImpl<>(List.of(loan));
        Pageable pageable = PageRequest.of(0, 10);
        when(loanRepository.findByAccountIn(List.of(account), pageable)).thenReturn(loans);
        when(loanMapper.toShortDto(loan)).thenReturn(loanShortDto);

        Page<LoanShortDto> result = loanService.getClientLoans(token, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals(loanShortDto.getLoanNumber(), result.getContent().get(0).getLoanNumber());
    }

    @Test
    void testGetLoanInstallments_ReturnsList() {
        when(installmentRepository.findByLoanId(loan.getId())).thenReturn(List.of(installment));
        when(installmentMapper.toDto(installment)).thenReturn(installmentDto);

        List<InstallmentDto> result = loanService.getLoanInstallments(loan.getId());
        assertEquals(1, result.size());
        assertEquals(installmentDto.getAmount(), result.get(0).getAmount());
    }

    @Test
    void testGetAllLoans_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Loan> page = new PageImpl<>(List.of(loan));
        when(loanRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(loanMapper.toDto(loan)).thenReturn(loanDto);

        Page<LoanDto> result = loanService.getAllLoans(LoanType.CASH, "12345", LoanStatus.APPROVED, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals(loanDto.getLoanNumber(), result.getContent().get(0).getLoanNumber());
    }

    @Test
    void testGetLoanInstallments_EmptyList() {
        when(installmentRepository.findByLoanId(99L)).thenReturn(Collections.emptyList());
        List<InstallmentDto> result = loanService.getLoanInstallments(99L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetClientLoans_EmptyResult() {
        String token = "Bearer xyz";
        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(1L);
        when(accountRepository.findByClientId(1L)).thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);
        when(loanRepository.findByAccountIn(Collections.emptyList(), pageable))
                .thenReturn(Page.empty());

        Page<LoanShortDto> result = loanService.getClientLoans(token, pageable);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueueDueInstallments_QueuesInstallmentsCorrectly() {
        loan.setNextInstallmentDate(LocalDate.now());
        loan.setStartDate(LocalDate.now().minusMonths(1));

        when(loanRepository.findByNextInstallmentDateAndStartDateBefore(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(loan));

        loanService.queueDueInstallments();

        verify(transactionQueueService, times(1)).queueTransaction(TransactionType.PAY_INSTALLMENT, loan.getId());
    }

    @Test
    void testQueueDueInstallments_NoLoans() {
        when(loanRepository.findByNextInstallmentDateAndStartDateBefore(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> loanService.queueDueInstallments());

        verify(transactionQueueService, never()).queueTransaction(any(), any());
    }

    @Test
    void testRetryLoanPayment_SuccessfulRetry() {
        // Setup valuta
        Currency currency = new Currency();
        currency.setCode("RSD");

        // Setup account i bank account
        account.setCurrency(currency);
        account.setBalance(new BigDecimal("100000"));
        account.setAvailableBalance(new BigDecimal("100000"));

        CompanyAccount bankAccount = new CompanyAccount();
        bankAccount.setCurrency(currency);
        bankAccount.setBalance(new BigDecimal("50000000"));
        bankAccount.setAvailableBalance(new BigDecimal("50000000"));

        // Setup loan
        loan.setInterestRateType(InterestRateType.VARIABLE);
        loan.setInstallments(new ArrayList<>(List.of(installment)));

        // Mockovi
        when(accountRepository.findByAccountNumber(account.getAccountNumber()))
                .thenReturn(Optional.of(account));

        when(accountRepository.findFirstByCurrencyAndCompanyId(currency, 1L))
                .thenReturn(Optional.of(bankAccount));

        // Poziv
        loanService.retryLoanPayment(loan);

        // Provere
        verify(accountRepository).save(account);
        verify(accountRepository).save(bankAccount);
        assertEquals(InstallmentStatus.PAID, installment.getInstallmentStatus());
        assertNotNull(installment.getActualDueDate());
    }

    @Test
    void testRetryLoanPayment_InsufficientFunds() {
        account.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));

        ClientDto clientDto = new ClientDto();
        clientDto.setEmail("client@example.com");
        clientDto.setId(account.getClientId());
        when(userClient.getClientById(account.getClientId())).thenReturn(clientDto);

        loan.setInstallments(new ArrayList<>(List.of(installment)));

        assertDoesNotThrow(() -> loanService.retryLoanPayment(loan));

        verify(loanRepository).save(loan);
        verify(scheduledExecutorService).schedule(any(Runnable.class), eq(72L), eq(TimeUnit.HOURS));
        assertEquals(loan.getNominalInterestRate(), BigDecimal.valueOf(5.55));
    }

    @Test
    void testPayInstallment_Success() {
        account.setCurrency(currency); // ← ključni deo da izbegneš null

        CompanyAccount bankAccount = new CompanyAccount();
        bankAccount.setCurrency(currency);
        bankAccount.setBalance(BigDecimal.valueOf(1000000));
        bankAccount.setAvailableBalance(BigDecimal.valueOf(1000000));

        loan.setInstallments(new ArrayList<>(List.of(installment)));
        loan.setStatus(LoanStatus.APPROVED);

        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(accountRepository.findFirstByCurrencyAndCompanyId(any(), eq(1L))).thenReturn(Optional.of(bankAccount));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(installmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> loanService.payInstallment(loan.getId()));

        verify(accountRepository, atLeastOnce()).save(any());
        verify(installmentRepository, times(2)).save(any());
        verify(loanRepository).save(any());
    }

    @Test
    void testPayInstallment_InsufficientFunds_Throws() {
        loan.setNextInstallmentAmount(BigDecimal.valueOf(100000));
        account.setBalance(BigDecimal.valueOf(1000));
        account.setAvailableBalance(BigDecimal.valueOf(1000));
        loan.setStatus(LoanStatus.APPROVED);

        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThrows(InsufficientFundsException.class, () -> loanService.payInstallment(loan.getId()));
    }

    @Test
    void testFindLoanIdByLoanRequestId_Success() {
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccount(account);

        Loan recentLoan = Loan.builder()
                .id(99L)
                .account(account)
                .startDate(LocalDate.now())
                .build();

        when(loanRequestRepository.findById(10L)).thenReturn(Optional.of(loanRequest));
        when(loanRepository.findAll()).thenReturn(List.of(loan, recentLoan));

        Long result = loanService.findLoanIdByLoanRequestId(10L);

        assertEquals(recentLoan.getId(), result);
    }

    @Test
    void testFindLoanIdByLoanRequestId_LoanNotFound() {
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAccount(account);

        lenient().when(loanRequestRepository.findById(10L)).thenReturn(Optional.of(loanRequest));
        when(loanRepository.findAll()).thenReturn(Collections.emptyList());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> loanService.findLoanIdByLoanRequestId(10L));
        assertTrue(ex.getMessage().contains("No loan found"));
    }
}
