package rs.raf.bank_service.unit;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import rs.raf.bank_service.domain.dto.CreateLoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.domain.mapper.LoanMapper;
import rs.raf.bank_service.domain.mapper.LoanRequestMapper;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.service.LoanRequestService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LoanRequestServiceTest {
    private LoanRequestRepository loanRequestRepository;
    private LoanRequestMapper loanRequestMapper;
    private AccountRepository accountRepository;
    private LoanRepository loanRepository;
    private LoanMapper loanMapper;
    private CurrencyRepository currencyRepository;
    private InstallmentRepository installmentRepository;
    private JwtTokenUtil jwtTokenUtil;

    private LoanRequestService loanRequestService;

    @BeforeEach
    void setUp() {
        loanRequestRepository = mock(LoanRequestRepository.class);
        loanRequestMapper = mock(LoanRequestMapper.class);
        accountRepository = mock(AccountRepository.class);
        loanRepository = mock(LoanRepository.class);
        loanMapper = mock(LoanMapper.class);
        currencyRepository = mock(CurrencyRepository.class);
        installmentRepository = mock(InstallmentRepository.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);

        loanRequestService = new LoanRequestService(
                loanRequestRepository,
                loanRequestMapper,
                accountRepository,
                loanRepository,
                loanMapper,
                currencyRepository,
                installmentRepository,
                jwtTokenUtil
        );
    }

    @Test
    void testSaveLoanRequest() {
        // Arrange
        String authHeader = "Bearer token";
        Long clientId = 1L;
        CreateLoanRequestDto createDto = new CreateLoanRequestDto();
        createDto.setCurrencyCode("EUR");
        createDto.setAccountNumber("123-456");

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setStatus(LoanRequestStatus.PENDING);

        Account account = new Account() {
        };
        Currency currency = new Currency();

        LoanRequestDto expectedDto = new LoanRequestDto();

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(loanRequestMapper.createRequestToEntity(createDto)).thenReturn(loanRequest);
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency));
        when(accountRepository.findByAccountNumberAndClientId("123-456", clientId)).thenReturn(Optional.of(account));
        when(loanRequestRepository.save(loanRequest)).thenReturn(loanRequest);
        when(loanRequestMapper.toDto(loanRequest)).thenReturn(expectedDto);

        // Act
        LoanRequestDto result = loanRequestService.saveLoanRequest(createDto, authHeader);

        // Assert
        assertEquals(expectedDto, result);
        assertEquals(LoanRequestStatus.PENDING, loanRequest.getStatus());

        verify(jwtTokenUtil).getUserIdFromAuthHeader(authHeader);
        verify(loanRequestMapper).createRequestToEntity(createDto);
        verify(currencyRepository).findByCode("EUR");
        verify(accountRepository).findByAccountNumberAndClientId("123-456", clientId);
        verify(loanRequestRepository).save(loanRequest);
        verify(loanRequestMapper).toDto(loanRequest);
    }

    @Test
    void testApproveLoan() {
        // Arrange
        Long loanRequestId = 1L;
        BigDecimal loanAmount = BigDecimal.valueOf(1000);

        Currency currency = new Currency();
        currency.setCode("EUR");

        Account clientAccount = new Account() {};
        clientAccount.setBalance(BigDecimal.valueOf(2000));
        clientAccount.setAvailableBalance(BigDecimal.valueOf(2000));
        clientAccount.setCurrency(currency);

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setId(loanRequestId);
        loanRequest.setStatus(LoanRequestStatus.PENDING);
        loanRequest.setAmount(loanAmount);
        loanRequest.setRepaymentPeriod(12);
        loanRequest.setType(LoanType.CASH);
        loanRequest.setCurrency(currency);
        loanRequest.setInterestRateType(InterestRateType.FIXED);
        loanRequest.setAccount(clientAccount);

        CompanyAccount bankAccount = new CompanyAccount();
        bankAccount.setAccountNumber("BANK-123");
        bankAccount.setBalance(BigDecimal.valueOf(50000));
        bankAccount.setAvailableBalance(BigDecimal.valueOf(50000));
        bankAccount.setCurrency(currency);
        bankAccount.setCompanyId(1L);

        Loan loan = Loan.builder().build();
        LoanDto loanDto = new LoanDto();

        // Mock
        when(loanRequestRepository.findByIdAndStatus(loanRequestId, LoanRequestStatus.PENDING)).thenReturn(Optional.of(loanRequest));
        when(accountRepository.findFirstByCurrencyAndCompanyId(currency, 1L)).thenReturn(Optional.of(bankAccount));
        when(loanRepository.save(Mockito.any(Loan.class))).thenReturn(loan);
        when(installmentRepository.save(Mockito.any(Installment.class))).thenReturn(new Installment());
        when(loanMapper.toDto(Mockito.any(Loan.class))).thenReturn(loanDto);

        // Act
        LoanDto result = loanRequestService.approveLoan(loanRequestId);

        // Assert
        assertEquals(loanDto, result);
        assertEquals(LoanRequestStatus.APPROVED, loanRequest.getStatus());

        // Balansi
        assertEquals(BigDecimal.valueOf(3000), clientAccount.getBalance());
        assertEquals(BigDecimal.valueOf(3000), clientAccount.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(49000), bankAccount.getBalance());

        // Verify pozivi
        verify(loanRequestRepository).findByIdAndStatus(loanRequestId, LoanRequestStatus.PENDING);
        verify(accountRepository).findFirstByCurrencyAndCompanyId(currency, 1L);
        verify(accountRepository).save(clientAccount);
        verify(accountRepository).save(bankAccount);
        verify(loanRepository, times(1)).save(Mockito.any(Loan.class));
        verify(installmentRepository, times(1)).save(Mockito.any(Installment.class));
        verify(loanMapper).toDto(Mockito.any(Loan.class));
    }

    @Test
    void testRejectLoan() {
        // Arrange
        Long loanRequestId = 1L;
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setId(loanRequestId);
        loanRequest.setStatus(LoanRequestStatus.PENDING);

        LoanRequestDto loanRequestDto = new LoanRequestDto();

        when(loanRequestRepository.findByIdAndStatus(loanRequestId, LoanRequestStatus.PENDING)).thenReturn(Optional.of(loanRequest));
        when(loanRequestRepository.save(loanRequest)).thenReturn(loanRequest);
        when(loanRequestMapper.toDto(loanRequest)).thenReturn(loanRequestDto);

        // Act
        LoanRequestDto result = loanRequestService.rejectLoan(loanRequestId);

        // Assert
        assertEquals(LoanRequestStatus.REJECTED, loanRequest.getStatus());
        assertEquals(loanRequestDto, result);
        verify(loanRequestRepository).findByIdAndStatus(loanRequestId, LoanRequestStatus.PENDING);
        verify(loanRequestRepository).save(loanRequest);
        verify(loanRequestMapper).toDto(loanRequest);
    }

}
