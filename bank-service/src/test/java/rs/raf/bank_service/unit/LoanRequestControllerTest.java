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
import rs.raf.bank_service.domain.dto.CreateLoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.service.LoanRequestService;
import rs.raf.bank_service.service.LoanService;
import rs.raf.bank_service.service.TransactionQueueService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Mock
    private TransactionQueueService transactionQueueService;


//    @Test
//    void testGetAllLoans() {
//        List<LoanShortDto> mockLoans = Arrays.asList(
//                new LoanShortDto("12345", LoanType.MORTGAGE, BigDecimal.valueOf(500000)),
//                new LoanShortDto("67890", LoanType.REFINANCING, BigDecimal.valueOf(1500000))
//        );
//
//        when(loanService.getAllLoans()).thenReturn(mockLoans);
//
//        ResponseEntity<List<LoanShortDto>> response = loanController.getAllLoans();
//
//        assertEquals(200, response.getStatusCodeValue());
//        assertNotNull(response.getBody());
//        assertEquals(2, response.getBody().size());
//    }

    @Test
    void testGetLoanById() {
        Long loanId = 1L;
        LoanDto mockLoan = new LoanDto(1L, "12345", LoanType.MORTGAGE, BigDecimal.valueOf(500000), 60, BigDecimal.valueOf(6.75), BigDecimal.valueOf(7.25), LocalDate.now(), LocalDate.now().plusYears(5), BigDecimal.valueOf(10000), LocalDate.now().plusMonths(1), BigDecimal.valueOf(450000), "RSD", LoanStatus.APPROVED);

        when(loanService.getLoanById(loanId)).thenReturn(Optional.of(mockLoan));

        ResponseEntity<LoanDto> response = loanController.getLoanById(loanId);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(mockLoan.getLoanNumber(), response.getBody().getLoanNumber());
    }


    @Test
    void testCreateLoanRequest() {
        CreateLoanRequestDto newRequest = new CreateLoanRequestDto(LoanType.REFINANCING, BigDecimal.valueOf(500000), "Education", BigDecimal.valueOf(85000), EmploymentStatus.PERMANENT, 36, 72, "+381641234567", "265000000333333333333", "USD", InterestRateType.FIXED);

        LoanRequestDto savedRequest = new LoanRequestDto(1L, LoanType.REFINANCING, BigDecimal.valueOf(500000), "Education", BigDecimal.valueOf(85000), EmploymentStatus.PERMANENT, 36, 72, "+381641234567", "265000000333333333333", "USD", InterestRateType.FIXED, LocalDateTime.now(), LoanRequestStatus.PENDING);
        when(loanRequestService.saveLoanRequest(newRequest, "Bearer token")).thenReturn(savedRequest);

        ResponseEntity<String> response = (ResponseEntity<String>) loanRequestController.createLoanRequest(newRequest, "Bearer token");

        assertEquals(201, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Loan request created successfully.", response.getBody());
    }

    @Test
    void approveLoan_Success() {
        Long loanRequestId = 1L;
        LoanDto loanDto = new LoanDto();

        Mockito.when(transactionQueueService.queueLoan("APPROVE_LOAN", loanRequestId)).thenReturn(loanDto);

        ResponseEntity<?> response = loanRequestController.approveLoan(loanRequestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(loanDto, response.getBody());
        Mockito.verify(transactionQueueService).queueLoan("APPROVE_LOAN", loanRequestId);
    }


//    @Test
//    void approveLoan_LoanNotFound() {
//        Long loanId = 1L;
//
//        Mockito.when(loanRequestService.approveLoan(loanId)).thenThrow(new LoanRequestNotFoundException());
//
//        ResponseEntity<?> response = loanRequestController.approveLoan(loanId);
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
}
