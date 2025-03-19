package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.LoanController;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.service.LoanService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;


    @Test
    void testGetLoanById() {
        Long loanId = 1L;
        LoanDto mockLoan = new LoanDto(1L, "12345", LoanType.AUTO, BigDecimal.valueOf(500000), 60, BigDecimal.valueOf(6.75), BigDecimal.valueOf(7.25), LocalDate.now(), LocalDate.now().plusYears(5), BigDecimal.valueOf(10000), LocalDate.now().plusMonths(1), BigDecimal.valueOf(450000), "RSD", LoanStatus.APPROVED);

        when(loanService.getLoanById(loanId)).thenReturn(Optional.of(mockLoan));

        ResponseEntity<LoanDto> response = loanController.getLoanById(loanId);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(mockLoan.getLoanNumber(), response.getBody().getLoanNumber());
    }

    @Test
    void testGetClientLoans() {
        String authHeader = "Bearer valid-token";
        int page = 0;
        int size = 10;

        Page<LoanShortDto> mockPage = Mockito.mock(Page.class);

        when(loanService.getClientLoans(Mockito.eq(authHeader), Mockito.any())).thenReturn(mockPage);

        ResponseEntity<Page<LoanShortDto>> response = loanController.getClientLoans(authHeader, page, size);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockPage, response.getBody());
    }


    @Test
    void testGetLoanInstallments() {
        Long loanId = 1L;

        List<InstallmentDto> mockInstallments = List.of(
                InstallmentDto.builder()
                        .amount(BigDecimal.valueOf(10000))
                        .interestRate(BigDecimal.valueOf(6.75))
                        .expectedDueDate(LocalDate.now())
                        .actualDueDate(LocalDate.now())
                        .installmentStatus(InstallmentStatus.LATE)
                        .build(),
                InstallmentDto.builder()
                        .amount(BigDecimal.valueOf(15000))
                        .interestRate(BigDecimal.valueOf(6.75))
                        .expectedDueDate(LocalDate.now().plusMonths(1))
                        .actualDueDate(LocalDate.now().plusMonths(1))
                        .installmentStatus(InstallmentStatus.PAID)
                        .build()
        );

        when(loanService.getLoanInstallments(loanId)).thenReturn(mockInstallments);

        ResponseEntity<List<InstallmentDto>> response = loanController.getLoanInstallments(loanId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(mockInstallments, response.getBody());
    }

    @Test
    void testGetAllLoans() {
        LoanDto mockLoan = new LoanDto();
        mockLoan.setLoanNumber("LN-123");
        mockLoan.setAmount(BigDecimal.valueOf(5000));
        mockLoan.setType(LoanType.AUTO);
        mockLoan.setStatus(LoanStatus.APPROVED);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("account.accountNumber").ascending());
        Page<LoanDto> mockPage = new PageImpl<>(List.of(mockLoan));

        // Koristi any() matchere za stubing
        when(loanService.getAllLoans(any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<Page<LoanDto>> response = loanController.getAllLoans(
                LoanType.AUTO, "123456789", LoanStatus.APPROVED, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("LN-123", response.getBody().getContent().get(0).getLoanNumber());

        // Verifikuj da je metoda pozvana sa bilo kojim argumentima
        verify(loanService, times(1)).getAllLoans(any(), any(), any(), any(Pageable.class));
    }


}
