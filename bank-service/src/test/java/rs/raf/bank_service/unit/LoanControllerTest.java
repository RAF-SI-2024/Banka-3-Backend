package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import rs.raf.bank_service.exceptions.InvalidLoanStatusException;
import rs.raf.bank_service.exceptions.InvalidLoanTypeException;
import rs.raf.bank_service.exceptions.LoanNotFoundException;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.service.LoanService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    @Test
    void testGetClientLoans_Success() {
        Page<LoanShortDto> mockPage = new PageImpl<>(List.of(new LoanShortDto()));
        when(loanService.getClientLoans(anyString(), any())).thenReturn(mockPage);

        ResponseEntity<Page<LoanShortDto>> response = loanController.getClientLoans("Bearer token", 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetClientLoans_Forbidden() {
        when(loanService.getClientLoans(anyString(), any())).thenThrow(new UnauthorizedException("Unauthorized"));

        ResponseEntity<Page<LoanShortDto>> response = loanController.getClientLoans("invalid", 0, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testGetClientLoans_GenericException() {
        when(loanService.getClientLoans(anyString(), any())).thenThrow(new RuntimeException());

        ResponseEntity<Page<LoanShortDto>> response = loanController.getClientLoans("something", 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testGetLoanInstallments_Success() {
        List<InstallmentDto> list = List.of(new InstallmentDto(BigDecimal.TEN, BigDecimal.ONE, LocalDate.now(), LocalDate.now(), InstallmentStatus.PAID));
        when(loanService.getLoanInstallments(1L)).thenReturn(list);

        ResponseEntity<List<InstallmentDto>> response = loanController.getLoanInstallments(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetLoanInstallments_NotFound() {
        when(loanService.getLoanInstallments(anyLong())).thenThrow(new LoanNotFoundException(1L));

        ResponseEntity<List<InstallmentDto>> response = loanController.getLoanInstallments(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetLoanById_Found() {
        LoanDto dto = new LoanDto();
        dto.setLoanNumber("LN-001");

        when(loanService.getLoanById(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<LoanDto> response = loanController.getLoanById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("LN-001", response.getBody().getLoanNumber());
    }

    @Test
    void testGetLoanById_NotFound() {
        when(loanService.getLoanById(2L)).thenReturn(Optional.empty());

        ResponseEntity<LoanDto> response = loanController.getLoanById(2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetAllLoans_Success() {
        Page<LoanDto> mockPage = new PageImpl<>(List.of(new LoanDto()));
        when(loanService.getAllLoans(any(), any(), any(), any())).thenReturn(mockPage);

        ResponseEntity<Page<LoanDto>> response = loanController.getAllLoans(LoanType.CASH, "12345", LoanStatus.APPROVED, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void testGetAllLoans_InvalidType() {
        when(loanService.getAllLoans(any(), any(), any(), any())).thenThrow(new InvalidLoanTypeException());

        ResponseEntity<Page<LoanDto>> response = loanController.getAllLoans(null, null, null, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleLoanNotFoundException() {
        ResponseEntity<String> response = loanController.handleLoanNotFoundException(new LoanNotFoundException(1L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testHandleUnauthorizedException() {
        ResponseEntity<String> response = loanController.handleUnauthorizedException(new UnauthorizedException("Denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testHandleInvalidLoanTypeException() {
        ResponseEntity<String> response = loanController.handleInvalidLoanTypeException(new InvalidLoanTypeException());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleInvalidLoanStatusException() {
        ResponseEntity<String> response = loanController.handleInvalidLoanStatusException(new InvalidLoanStatusException());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testHandleGenericException() {
        ResponseEntity<String> response = loanController.handleGenericException(new RuntimeException("Something went wrong"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
