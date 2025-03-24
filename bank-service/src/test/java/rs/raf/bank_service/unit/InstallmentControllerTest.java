package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.InstallmentController;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.service.InstallmentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstallmentControllerTest {

    @Mock
    private InstallmentService installmentService;

    @InjectMocks
    private InstallmentController installmentController;

    @Test
    void testGetInstallmentsByLoanId_ReturnsList() {
        Long loanId = 1L;
        List<InstallmentDto> mockInstallments = List.of(
                new InstallmentDto(BigDecimal.valueOf(10000), BigDecimal.valueOf(5.5), LocalDate.now(), LocalDate.now().plusMonths(1), InstallmentStatus.LATE),
                new InstallmentDto(BigDecimal.valueOf(12000), BigDecimal.valueOf(5.5), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(2), InstallmentStatus.UNPAID)
        );

        when(installmentService.getInstallmentsByLoanId(loanId)).thenReturn(mockInstallments);

        ResponseEntity<List<InstallmentDto>> response = installmentController.getInstallmentsByLoanId(loanId);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(installmentService, times(1)).getInstallmentsByLoanId(loanId);
    }

    @Test
    void testGetInstallmentsByLoanId_ReturnsEmptyList() {
        Long loanId = 999L;
        when(installmentService.getInstallmentsByLoanId(loanId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<InstallmentDto>> response = installmentController.getInstallmentsByLoanId(loanId);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(installmentService, times(1)).getInstallmentsByLoanId(loanId);
    }

}
