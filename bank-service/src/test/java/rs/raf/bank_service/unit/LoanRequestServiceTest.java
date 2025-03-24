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
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.service.LoanRequestService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        String authHeader = "Bearer token";
        Long clientId = 1L;
        CreateLoanRequestDto createDto = new CreateLoanRequestDto();
        createDto.setCurrencyCode("EUR");
        createDto.setAccountNumber("123-456");

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setStatus(LoanRequestStatus.PENDING);

        Account account = new Account() {};
        Currency currency = new Currency();

        LoanRequestDto expectedDto = new LoanRequestDto();

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(loanRequestMapper.createRequestToEntity(createDto)).thenReturn(loanRequest);
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency));
        when(accountRepository.findByAccountNumberAndClientId("123-456", clientId)).thenReturn(Optional.of(account));
        when(loanRequestRepository.save(loanRequest)).thenReturn(loanRequest);
        when(loanRequestMapper.toDto(loanRequest)).thenReturn(expectedDto);

        LoanRequestDto result = loanRequestService.saveLoanRequest(createDto, authHeader);

        assertEquals(expectedDto, result);
        assertEquals(LoanRequestStatus.PENDING, loanRequest.getStatus());
    }

    @Test
    void testSaveLoanRequest_InvalidCurrency() {
        String authHeader = "Bearer token";
        Long clientId = 1L;

        CreateLoanRequestDto createDto = new CreateLoanRequestDto();
        createDto.setCurrencyCode("XXX");
        createDto.setAccountNumber("123456789012345678");

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(loanRequestMapper.createRequestToEntity(createDto)).thenReturn(new LoanRequest());
        when(currencyRepository.findByCode("XXX")).thenReturn(Optional.empty());


        assertThrows(CurrencyNotFoundException.class, () ->
                loanRequestService.saveLoanRequest(createDto, authHeader));
    }


    @Test
    void testSaveLoanRequest_AccountNotFound() {

        String authHeader = "Bearer token";
        Long clientId = 1L;

        CreateLoanRequestDto createDto = new CreateLoanRequestDto();
        createDto.setCurrencyCode("RSD");
        createDto.setAccountNumber("123456789012345678");

        Currency currency = new Currency();

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(loanRequestMapper.createRequestToEntity(createDto)).thenReturn(new LoanRequest());
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(currency));
        when(accountRepository.findByAccountNumberAndClientId("123456789012345678", clientId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                loanRequestService.saveLoanRequest(createDto, authHeader));
    }

    @Test
    void testApproveLoan_RequestNotFound() {
        when(loanRequestRepository.findByIdAndStatus(1L, LoanRequestStatus.PENDING)).thenReturn(Optional.empty());
        assertThrows(LoanRequestNotFoundException.class, () -> loanRequestService.approveLoan(1L));
    }

    @Test
    void testRejectLoan_RequestNotFound() {
        when(loanRequestRepository.findByIdAndStatus(1L, LoanRequestStatus.PENDING)).thenReturn(Optional.empty());
        assertThrows(LoanRequestNotFoundException.class, () -> loanRequestService.rejectLoan(1L));
    }

    @Test
    void testApproveLoan() {
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

        when(loanRequestRepository.findByIdAndStatus(loanRequestId, LoanRequestStatus.PENDING)).thenReturn(Optional.of(loanRequest));
        when(accountRepository.findFirstByCurrencyAndCompanyId(currency, 1L)).thenReturn(Optional.of(bankAccount));
        when(loanRepository.save(Mockito.any(Loan.class))).thenReturn(loan);
        when(installmentRepository.save(Mockito.any(Installment.class))).thenReturn(new Installment());
        when(loanMapper.toDto(Mockito.any(Loan.class))).thenReturn(loanDto);

        LoanDto result = loanRequestService.approveLoan(loanRequestId);

        assertEquals(loanDto, result);
        assertEquals(LoanRequestStatus.APPROVED, loanRequest.getStatus());
        assertEquals(BigDecimal.valueOf(3000), clientAccount.getBalance());
        assertEquals(BigDecimal.valueOf(3000), clientAccount.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(49000), bankAccount.getBalance());
    }

    @Test
    void testRejectLoan() {
        Long loanRequestId = 1L;
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setId(loanRequestId);
        loanRequest.setStatus(LoanRequestStatus.PENDING);

        LoanRequestDto loanRequestDto = new LoanRequestDto();

        when(loanRequestRepository.findByIdAndStatus(loanRequestId, LoanRequestStatus.PENDING)).thenReturn(Optional.of(loanRequest));
        when(loanRequestRepository.save(loanRequest)).thenReturn(loanRequest);
        when(loanRequestMapper.toDto(loanRequest)).thenReturn(loanRequestDto);

        LoanRequestDto result = loanRequestService.rejectLoan(loanRequestId);

        assertEquals(LoanRequestStatus.REJECTED, loanRequest.getStatus());
        assertEquals(loanRequestDto, result);
    }
}
