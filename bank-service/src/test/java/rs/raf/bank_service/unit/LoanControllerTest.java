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
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.exceptions.LoanRequestNotFoundException;
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
public class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    @Test
    void testGetAllLoans() {
        List<LoanShortDto> mockLoans = Arrays.asList(
                new LoanShortDto("12345", LoanType.AUTO, BigDecimal.valueOf(500000)),
                new LoanShortDto("67890", LoanType.CASH, BigDecimal.valueOf(1500000))
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
        LoanDto mockLoan = new LoanDto("12345", LoanType.AUTO, BigDecimal.valueOf(500000), 60, BigDecimal.valueOf(6.75), BigDecimal.valueOf(7.25), LocalDate.now(), LocalDate.now().plusYears(5), BigDecimal.valueOf(10000), LocalDate.now().plusMonths(1), BigDecimal.valueOf(450000), "RSD", LoanStatus.APPROVED);

        when(loanService.getLoanById(loanId)).thenReturn(Optional.of(mockLoan));

        ResponseEntity<LoanDto> response = loanController.getLoanById(loanId);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(mockLoan.getLoanNumber(), response.getBody().getLoanNumber());
    }

}
