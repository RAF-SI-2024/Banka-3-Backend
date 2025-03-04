package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.TransactionStatus;
import rs.raf.bank_service.domain.mapper.PaymentReviewMapper;
import rs.raf.bank_service.exceptions.PaymentNotFoundException;
import rs.raf.bank_service.repository.PaymentReviewRepository;
import rs.raf.bank_service.service.PaymentReviewService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentReviewServiceTest {

    @Mock
    private PaymentReviewRepository repository;

    @Mock
    private PaymentReviewMapper mapper;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private PaymentReviewService service;

    @Test
    public void testGetPayments_Success() {
        // Arrange
        String token = "validToken";
        Long clientId = 1L;
        LocalDateTime startDate = LocalDateTime.of(2023, 10, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 10, 31, 23, 59);
        BigDecimal minAmount = new BigDecimal("100");
        BigDecimal maxAmount = new BigDecimal("1000");
        TransactionStatus paymentStatus = TransactionStatus.COMPLETED;

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setClientId(clientId);
        payment.setAmount(new BigDecimal("500"));
        payment.setTransactionDate(LocalDateTime.now());
        payment.setPaymentStatus(TransactionStatus.COMPLETED);

        PaymentOverviewDto overviewDto = new PaymentOverviewDto();
        overviewDto.setId(1L);
        overviewDto.setAmount(new BigDecimal("500"));
        overviewDto.setTransactionDate(LocalDateTime.now());
        overviewDto.setPaymentStatus(TransactionStatus.COMPLETED);

        when(jwtTokenUtil.validateToken(token.replace("Bearer ", ""))).thenReturn(true);
        when(jwtTokenUtil.extractUserId(token.replace("Bearer ", ""))).thenReturn(clientId.toString());
        when(repository.findByClientIdAndTransactionDateBetweenAndAmountBetweenAndPaymentStatus(
                clientId, startDate, endDate, minAmount, maxAmount, paymentStatus
        )).thenReturn(List.of(payment));
        when(mapper.toOverviewDto(payment)).thenReturn(overviewDto);

        // Act
        List<PaymentOverviewDto> result = service.getPayments(token, startDate, endDate, minAmount, maxAmount, paymentStatus);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(overviewDto.getId(), result.get(0).getId());
        verify(repository, times(1)).findByClientIdAndTransactionDateBetweenAndAmountBetweenAndPaymentStatus(
                clientId, startDate, endDate, minAmount, maxAmount, paymentStatus
        );
    }

    @Test
    public void testGetPaymentDetails_Success() {
        // Arrange
        Long id = 1L;
        Payment payment = new Payment();
        payment.setId(id);
        payment.setAmount(new BigDecimal("500"));
        payment.setTransactionDate(LocalDateTime.now());
        payment.setPaymentStatus(TransactionStatus.COMPLETED);

        PaymentDetailsDto detailsDto = new PaymentDetailsDto();
        detailsDto.setId(id);
        detailsDto.setAmount(new BigDecimal("500"));
        detailsDto.setTransactionDate(LocalDateTime.now());
        detailsDto.setPaymentStatus(TransactionStatus.COMPLETED);

        when(repository.findById(id)).thenReturn(Optional.of(payment));
        when(mapper.toDetailsDto(payment)).thenReturn(detailsDto);

        // Act
        PaymentDetailsDto result = service.getPaymentDetails(id);

        // Assert
        assertNotNull(result);
        assertEquals(detailsDto.getId(), result.getId());
        verify(repository, times(1)).findById(id);
    }

    @Test
    public void testGetPaymentDetails_PaymentNotFound() {
        // Arrange
        Long id = 1L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> {
            service.getPaymentDetails(id);
        });
        verify(repository, times(1)).findById(id);
    }
}
