package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.LoanController;
import rs.raf.bank_service.controller.LoanRequestController;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.enums.EmploymentStatus;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.service.LoanRequestService;
import rs.raf.bank_service.service.LoanService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoanRequestControllerTest {

    /*
    @Mock
    private LoanService loanService;

    @Mock
    private LoanRequestService loanRequestService;

    @InjectMocks
    private LoanController loanController;

    @InjectMocks
    private LoanRequestController loanRequestController;

    @Test
    void testGetAllLoans() {
        List<LoanShortDto> mockLoans = Arrays.asList(
                new LoanShortDto("12345", LoanType.MORTGAGE, BigDecimal.valueOf(500000)),
                new LoanShortDto("67890", LoanType.REFINANCING, BigDecimal.valueOf(1500000))
        );

        when(loanService.getAllLoans()).thenReturn(mockLoans);

        ResponseEntity<List<LoanShortDto>> response = loanController.getAllLoans();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testGetLoanById() {
        Long loanId = 1L;
        LoanDto mockLoan = new LoanDto("12345", LoanType.MORTGAGE, BigDecimal.valueOf(500000), 60, BigDecimal.valueOf(6.75), BigDecimal.valueOf(7.25), LocalDate.now(), LocalDate.now().plusYears(5), BigDecimal.valueOf(10000), LocalDate.now().plusMonths(1), BigDecimal.valueOf(450000), "RSD", LoanStatus.APPROVED);

        when(loanService.getLoanById(loanId)).thenReturn(Optional.of(mockLoan));

        ResponseEntity<LoanDto> response = loanController.getLoanById(loanId);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(mockLoan.getLoanNumber(), response.getBody().getLoanNumber());
    }

    @Test
    void testCreateLoan() {
        LoanDto newLoan = new LoanDto("54321", LoanType.REFINANCING, BigDecimal.valueOf(300000), 48, BigDecimal.valueOf(5.5), BigDecimal.valueOf(6.0), LocalDate.now(), LocalDate.now().plusYears(4), BigDecimal.valueOf(8000), LocalDate.now().plusMonths(1), BigDecimal.valueOf(250000), "EUR", LoanStatus.APPROVED);
        when(loanService.saveLoan(newLoan)).thenReturn(newLoan);

        ResponseEntity<LoanDto> response = loanController.createLoan(newLoan);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(newLoan.getLoanNumber(), response.getBody().getLoanNumber());
    }

    @Test
    void testGetLoanRequestsByStatus() {
        LoanRequestStatus status = LoanRequestStatus.PENDING;
        List<LoanRequestDto> mockRequests = Arrays.asList(
                new LoanRequestDto(LoanType.CASH, BigDecimal.valueOf(500000), "Home Renovation", BigDecimal.valueOf(75000), EmploymentStatus.PERMANENT, 24, 60, "+381641234567", "265000000111111111111", "RSD", LoanRequestStatus.PENDING),
                new LoanRequestDto(LoanType.REFINANCING, BigDecimal.valueOf(300000), "Car Purchase", BigDecimal.valueOf(65000), EmploymentStatus.UNEMPLOYED, 12, 48, "+381641234567", "265000000222222222222", "EUR", LoanRequestStatus.APPROVED)
        );

        when(loanRequestService.getLoanRequestsByStatus(status)).thenReturn(mockRequests);

        ResponseEntity<List<LoanRequestDto>> response = loanRequestController.getLoanRequestsByStatus(status);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testCreateLoanRequest() {
        LoanRequestDto newRequest = new LoanRequestDto(LoanType.REFINANCING, BigDecimal.valueOf(500000), "Education", BigDecimal.valueOf(85000), EmploymentStatus.PERMANENT, 36, 72, "+381641234567", "265000000333333333333", "USD", LoanRequestStatus.APPROVED);
        when(loanRequestService.saveLoanRequest(newRequest)).thenReturn(newRequest);

        ResponseEntity<LoanRequestDto> response = loanRequestController.createLoanRequest(newRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(newRequest.getAmount(), response.getBody().getAmount());
    }

     */
}
