package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.LoanRequestController;
import rs.raf.bank_service.domain.dto.CreateLoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.enums.EmploymentStatus;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.exceptions.InvalidLoanTypeException;
import rs.raf.bank_service.exceptions.LoanRequestNotFoundException;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.service.LoanRequestService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanRequestControllerTest {

    @Mock
    private LoanRequestService loanRequestService;

    @InjectMocks
    private LoanRequestController controller;

    @Test
    void testCreateLoanRequest_Success() {
        CreateLoanRequestDto dto = new CreateLoanRequestDto(LoanType.CASH, new BigDecimal("5000"), "Laptop",
                new BigDecimal("100000"), EmploymentStatus.PERMANENT, 12, 24, "+38164123456", "265000000000000000",
                "RSD", InterestRateType.FIXED);

        when(loanRequestService.saveLoanRequest(eq(dto), eq("Bearer token"))).thenReturn(new LoanRequestDto());

        ResponseEntity<?> response = controller.createLoanRequest(dto, "Bearer token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Loan request created successfully"));
    }

    @Test
    void testCreateLoanRequest_InvalidLoanType() {
        CreateLoanRequestDto dto = new CreateLoanRequestDto();
        when(loanRequestService.saveLoanRequest(any(), any())).thenThrow(new InvalidLoanTypeException());

        ResponseEntity<?> response = controller.createLoanRequest(dto, "Bearer token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("The provided loan type is invalid.", response.getBody());

    }

    @Test
    void testApproveLoan_Success() {
        LoanDto dto = new LoanDto();
        when(loanRequestService.approveLoan(1L)).thenReturn(dto);

        ResponseEntity<?> response = controller.approveLoan(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testApproveLoan_NotFound() {
        when(loanRequestService.approveLoan(2L)).thenThrow(new LoanRequestNotFoundException());

        ResponseEntity<?> response = controller.approveLoan(2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("The provided loan request doesnt exist.", response.getBody());
    }

    @Test
    void testRejectLoan_Success() {
        LoanRequestDto dto = new LoanRequestDto();
        when(loanRequestService.rejectLoan(1L)).thenReturn(dto);

        ResponseEntity<LoanRequestDto> response = controller.rejectLoan(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testRejectLoan_NotFound() {
        when(loanRequestService.rejectLoan(1L)).thenThrow(new LoanRequestNotFoundException());

        ResponseEntity<LoanRequestDto> response = controller.rejectLoan(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetClientLoanRequests_Success() {
        Page<LoanRequestDto> page = new PageImpl<>(List.of(new LoanRequestDto()));
        when(loanRequestService.getClientLoanRequests(eq("Bearer token"), any())).thenReturn(page);

        ResponseEntity<?> response = controller.getClientLoanRequests("Bearer token", 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
    }

    @Test
    void testGetClientLoanRequests_Unauthorized() {
        when(loanRequestService.getClientLoanRequests(any(), any())).thenThrow(new UnauthorizedException("No access"));

        ResponseEntity<?> response = controller.getClientLoanRequests("bad-token", 0, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No access", response.getBody());
    }

    @Test
    void testGetAllLoanRequests_Success() {
        Page<LoanRequestDto> page = new PageImpl<>(List.of(new LoanRequestDto()));
        when(loanRequestService.getAllLoanRequests(any(), any(), any())).thenReturn(page);

        ResponseEntity<Page<LoanRequestDto>> response = controller.getAllLoanRequests(null, null, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void testGetAllLoanRequests_InvalidType() {
        when(loanRequestService.getAllLoanRequests(any(), any(), any())).thenThrow(new InvalidLoanTypeException());

        ResponseEntity<Page<LoanRequestDto>> response = controller.getAllLoanRequests(null, null, 0, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
