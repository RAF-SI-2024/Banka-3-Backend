package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.domain.mapper.InstallmentMapper;
import rs.raf.bank_service.repository.InstallmentRepository;
import rs.raf.bank_service.service.InstallmentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstallmentServiceTest {

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private InstallmentMapper installmentMapper;

    @InjectMocks
    private InstallmentService installmentService;

    private Installment installment;
    private InstallmentDto installmentDto;

    @BeforeEach
    void setUp() {
        installment = Installment.builder()
                .amount(BigDecimal.valueOf(10000))
                .interestRate(BigDecimal.valueOf(5.5))
                .expectedDueDate(LocalDate.now())
                .actualDueDate(LocalDate.now().plusMonths(1))
                .installmentStatus(InstallmentStatus.UNPAID)
                .build();

        installmentDto = InstallmentDto.builder()
                .amount(BigDecimal.valueOf(10000))
                .interestRate(BigDecimal.valueOf(5.5))
                .expectedDueDate(LocalDate.now())
                .actualDueDate(LocalDate.now().plusMonths(1))
                .installmentStatus(InstallmentStatus.UNPAID)
                .build();
    }

    @Test
    void testGetInstallmentsByLoanId_Success() {
        when(installmentRepository.findByLoanId(1L)).thenReturn(List.of(installment));
        when(installmentMapper.toDtoList(List.of(installment))).thenReturn(List.of(installmentDto));

        List<InstallmentDto> result = installmentService.getInstallmentsByLoanId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(installmentDto.getAmount(), result.get(0).getAmount());
    }

    @Test
    void testGetInstallmentsByLoanId_EmptyList() {
        when(installmentRepository.findByLoanId(1L)).thenReturn(Collections.emptyList());
        when(installmentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<InstallmentDto> result = installmentService.getInstallmentsByLoanId(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetInstallmentsByLoanId_Exception() {
        when(installmentRepository.findByLoanId(1L)).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> installmentService.getInstallmentsByLoanId(1L));
    }
}
