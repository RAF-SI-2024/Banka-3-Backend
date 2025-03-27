package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.repository.LoanRepository;
import rs.raf.bank_service.specification.LoanInterestRateCalculator;
import rs.raf.bank_service.specification.LoanScheduler;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanSchedulerTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanScheduler loanScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateVariableInterestRates_VariableLoan() {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setInterestRateType(InterestRateType.VARIABLE);
        loan.setNominalInterestRate(BigDecimal.valueOf(5));
        loan.setAmount(BigDecimal.valueOf(10000));
        loan.setType(rs.raf.bank_service.domain.enums.LoanType.CASH);

        when(loanRepository.findByStatus(LoanStatus.APPROVED)).thenReturn(List.of(loan));

        try (MockedStatic<LoanInterestRateCalculator> calculatorMock = mockStatic(LoanInterestRateCalculator.class)) {
            calculatorMock.when(() -> LoanInterestRateCalculator.calculateEffectiveRate(any(LoanRequest.class)))
                    .thenReturn(BigDecimal.valueOf(6.5));

            loanScheduler.updateVariableInterestRates();

            assertEquals(BigDecimal.valueOf(6.5), loan.getEffectiveInterestRate());
            assertNotNull(loan.getNominalInterestRate());
            verify(loanRepository, times(1)).save(loan);
        }
    }

    @Test
    void testUpdateVariableInterestRates_FixedLoanIgnored() {
        Loan loan = new Loan();
        loan.setStatus(LoanStatus.APPROVED);
        loan.setInterestRateType(InterestRateType.FIXED);

        when(loanRepository.findByStatus(LoanStatus.APPROVED)).thenReturn(List.of(loan));

        loanScheduler.updateVariableInterestRates();

        verify(loanRepository, never()).save(any());
    }

    @Test
    void testUpdateVariableInterestRates_NoLoans() {
        when(loanRepository.findByStatus(LoanStatus.APPROVED)).thenReturn(List.of());

        loanScheduler.updateVariableInterestRates();

        verify(loanRepository, never()).save(any());
    }

    @Test
    void testUpdateVariableInterestRates_MultipleLoans_OnlyVariableSaved() {
        Loan variableLoan = new Loan();
        variableLoan.setStatus(LoanStatus.APPROVED);
        variableLoan.setInterestRateType(InterestRateType.VARIABLE);
        variableLoan.setNominalInterestRate(BigDecimal.valueOf(4));
        variableLoan.setAmount(BigDecimal.valueOf(9000));
        variableLoan.setType(rs.raf.bank_service.domain.enums.LoanType.CASH);

        Loan fixedLoan = new Loan();
        fixedLoan.setStatus(LoanStatus.APPROVED);
        fixedLoan.setInterestRateType(InterestRateType.FIXED);

        when(loanRepository.findByStatus(LoanStatus.APPROVED)).thenReturn(List.of(variableLoan, fixedLoan));

        try (MockedStatic<LoanInterestRateCalculator> calculatorMock = mockStatic(LoanInterestRateCalculator.class)) {
            calculatorMock.when(() -> LoanInterestRateCalculator.calculateEffectiveRate(any(LoanRequest.class)))
                    .thenReturn(BigDecimal.valueOf(5.25));

            loanScheduler.updateVariableInterestRates();

            assertEquals(BigDecimal.valueOf(5.25), variableLoan.getEffectiveInterestRate());
            verify(loanRepository, times(1)).save(variableLoan);
            verify(loanRepository, never()).save(fixedLoan);
        }
    }

    @Test
    void testUpdateVariableInterestRates_NullNominalInterest() {
        Loan loan = new Loan();
        loan.setStatus(LoanStatus.APPROVED);
        loan.setInterestRateType(InterestRateType.VARIABLE);
        loan.setNominalInterestRate(null); // intentionally null
        loan.setAmount(BigDecimal.valueOf(10000));
        loan.setType(rs.raf.bank_service.domain.enums.LoanType.CASH);

        when(loanRepository.findByStatus(LoanStatus.APPROVED)).thenReturn(List.of(loan));

        try (MockedStatic<LoanInterestRateCalculator> calculatorMock = mockStatic(LoanInterestRateCalculator.class)) {
            calculatorMock.when(() -> LoanInterestRateCalculator.calculateEffectiveRate(any(LoanRequest.class)))
                    .thenReturn(BigDecimal.valueOf(5.75));

            assertThrows(NullPointerException.class, () -> loanScheduler.updateVariableInterestRates());
            verify(loanRepository, never()).save(any());
        }
    }
}
