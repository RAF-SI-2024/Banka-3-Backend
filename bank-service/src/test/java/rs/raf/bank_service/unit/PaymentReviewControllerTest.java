package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.PaymentReviewController;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.enums.TransactionStatus;
import rs.raf.bank_service.exceptions.PaymentNotFoundException;
import rs.raf.bank_service.service.PaymentReviewService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentReviewControllerTest {

    @Mock
    private PaymentReviewService service;

    @InjectMocks
    private PaymentReviewController controller;

    @Test
    public void testGetPayments_Success() {
        // Arrange
        String token = "validToken";
        LocalDateTime startDate = LocalDateTime.of(2023, 10, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 10, 31, 23, 59);
        BigDecimal minAmount = new BigDecimal("100");
        BigDecimal maxAmount = new BigDecimal("1000");
        TransactionStatus paymentStatus = TransactionStatus.COMPLETED;

        PaymentOverviewDto overviewDto = new PaymentOverviewDto();
        overviewDto.setId(1L);
        overviewDto.setAmount(new BigDecimal("500"));
        overviewDto.setTransactionDate(LocalDateTime.now());
        overviewDto.setPaymentStatus(TransactionStatus.COMPLETED);

        when(service.getPayments(token, startDate, endDate, minAmount, maxAmount, paymentStatus))
                .thenReturn(List.of(overviewDto));

        // Act
        ResponseEntity<List<PaymentOverviewDto>> response = controller.getPayments(token, startDate, endDate, minAmount, maxAmount, paymentStatus);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(overviewDto.getId(), response.getBody().get(0).getId());
    }

    @Test
    public void testGetPaymentDetails_Success() {
        // Arrange
        Long id = 1L;
        PaymentDetailsDto detailsDto = new PaymentDetailsDto();
        detailsDto.setId(id);
        detailsDto.setAmount(new BigDecimal("500"));
        detailsDto.setTransactionDate(LocalDateTime.now());
        detailsDto.setPaymentStatus(TransactionStatus.COMPLETED);

        when(service.getPaymentDetails(id)).thenReturn(detailsDto);

        // Act
        ResponseEntity<PaymentDetailsDto> response = controller.getPaymentDetails(id);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detailsDto.getId(), response.getBody().getId());
    }


    @Test
    public void testGetPaymentDetails_PaymentNotFound() {
        // Arrange
        Long id = 1L;

        when(service.getPaymentDetails(id)).thenThrow(new PaymentNotFoundException(id));

        // Act
        ResponseEntity<PaymentDetailsDto> response = controller.getPaymentDetails(id);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Payment not found with id: " + id, response.getBody());
    }

}
