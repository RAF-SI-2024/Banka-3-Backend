package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.LoanController;
import rs.raf.bank_service.controller.LoanRequestController;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.exceptions.LoanRequestNotFoundException;
import rs.raf.bank_service.service.LoanRequestService;
import rs.raf.bank_service.service.LoanService;
import rs.raf.bank_service.utils.JwtTokenUtil;

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

    @Mock
    private LoanService loanService;

    @Mock
    private LoanRequestService loanRequestService;

    @InjectMocks
    private LoanController loanController;

    @InjectMocks
    private LoanRequestController loanRequestController;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

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
    void testGetLoanRequestsByStatus() {
        LoanRequestStatus status = LoanRequestStatus.PENDING;
        List<LoanRequestDto> mockRequests = Arrays.asList(
                new LoanRequestDto(LoanType.CASH, BigDecimal.valueOf(500000), "Home Renovation", BigDecimal.valueOf(75000), EmploymentStatus.PERMANENT, 24, 60, "+381641234567", "265000000111111111111", "RSD", InterestRateType.FIXED),
                new LoanRequestDto(LoanType.REFINANCING, BigDecimal.valueOf(300000), "Car Purchase", BigDecimal.valueOf(65000), EmploymentStatus.UNEMPLOYED, 12, 48, "+381641234567", "265000000222222222222", "EUR", InterestRateType.FIXED)
        );

        when(loanRequestService.getLoanRequestsByStatus(status)).thenReturn(mockRequests);

        ResponseEntity<List<LoanRequestDto>> response = loanRequestController.getLoanRequestsByStatus(status);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testCreateLoanRequest() {
        LoanRequestDto newRequest = new LoanRequestDto(LoanType.REFINANCING, BigDecimal.valueOf(500000), "Education", BigDecimal.valueOf(85000), EmploymentStatus.PERMANENT, 36, 72, "+381641234567", "265000000333333333333", "USD", InterestRateType.FIXED);
        when(loanRequestService.saveLoanRequest(newRequest, "Bearer token")).thenReturn(newRequest);

        ResponseEntity<LoanRequestDto> response = (ResponseEntity<LoanRequestDto>) loanRequestController.createLoanRequest(newRequest, "Bearer token");

        assertEquals(201, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(newRequest.getAmount(), response.getBody().getAmount());
    }

    @Test
    void approveLoan_Success() {
        Long loanId = 1L;
        LoanDto loanDto = new LoanDto();
        Mockito.when(loanRequestService.approveLoan(loanId)).thenReturn(loanDto);

        ResponseEntity<?> response = loanRequestController.approveLoan(loanId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(loanDto, response.getBody());
    }

    @Test
    void approveLoan_LoanNotFound() {
        Long loanId = 1L;
        String errorMessage = "The provided loan request doesnt exist.";

        Mockito.when(loanRequestService.approveLoan(loanId)).thenThrow(new LoanRequestNotFoundException());

        ResponseEntity<?> response = loanRequestController.approveLoan(loanId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
    }
}
