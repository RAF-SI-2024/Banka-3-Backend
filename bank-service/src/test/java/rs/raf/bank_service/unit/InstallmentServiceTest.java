package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.mapper.InstallmentMapper;
import rs.raf.bank_service.repository.InstallmentRepository;
import rs.raf.bank_service.service.InstallmentService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InstallmentServiceTest {

    private InstallmentRepository installmentRepository;
    private InstallmentMapper installmentMapper;
    private InstallmentService installmentService;

    @BeforeEach
    void setUp() {
        installmentRepository = mock(InstallmentRepository.class);
        installmentMapper = mock(InstallmentMapper.class);
        installmentService = new InstallmentService(installmentRepository, installmentMapper);
    }

    @Test
    void testGetInstallmentsByLoanId() {
        // Arrange
        Long loanId = 1L;

        List<Installment> installments = Arrays.asList(
                new Installment(), new Installment()
        );

        List<InstallmentDto> installmentDtos = Arrays.asList(
                new InstallmentDto(), new InstallmentDto()
        );

        when(installmentRepository.findByLoanId(loanId)).thenReturn(installments);
        when(installmentMapper.toDtoList(installments)).thenReturn(installmentDtos);

        // Act
        List<InstallmentDto> result = installmentService.getInstallmentsByLoanId(loanId);

        // Assert
        assertEquals(installmentDtos.size(), result.size());
        assertEquals(installmentDtos, result);

        verify(installmentRepository, times(1)).findByLoanId(loanId);
        verify(installmentMapper, times(1)).toDtoList(installments);
    }
}
