package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.exceptions.CardNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.domain.mapper.PaymentMapper;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PaymentMapper paymentMapper; // Dodajte ovo polje ako koristite mapper

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPaymentsFilterByClientIdTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setClientId(clientId);
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.COMPLETED);

        Page<Payment> paymentPage = new PageImpl<>(Collections.singletonList(payment));
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paymentPage);

        when(paymentMapper.toOverviewDto(any(Payment.class))).thenReturn(new PaymentOverviewDto());

        // Act
        Page<PaymentOverviewDto> result = paymentService.getPayments(
                token, null, null, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(paymentRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPaymentsFilterByCardNumberTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        String cardNumber = "1234123412341234";
        Pageable pageable = PageRequest.of(0, 10);

        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);
        when(cardRepository.findByCardNumber(cardNumber))
                .thenReturn(Optional.of(new Card()));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setClientId(clientId);
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.COMPLETED);

        Page<Payment> paymentPage = new PageImpl<>(Collections.singletonList(payment));
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paymentPage);

        when(paymentMapper.toOverviewDto(any(Payment.class))).thenReturn(new PaymentOverviewDto());

        // Act
        Page<PaymentOverviewDto> result = paymentService.getPayments(
                token, null, null, null, null, null, null, cardNumber, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(paymentRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getPaymentsInvalidCardNumberTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        String invalidCardNumber = "0000000000000000";
        Pageable pageable = PageRequest.of(0, 10);

        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);
        when(cardRepository.findByCardNumber(invalidCardNumber))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> {
            paymentService.getPayments(
                    token, null, null, null, null, null, null, invalidCardNumber, pageable);
        });
    }
    @Test
    void getPaymentsFilterByAccountNumberTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        String accountNumber = "123456789";
        Pageable pageable = PageRequest.of(0, 10);


        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);


        Account acc = new Account() {
        };
        acc.setAccountNumber(accountNumber);
        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(acc));


        Payment payment = new Payment();
        payment.setId(1L);
        payment.setClientId(clientId);
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.COMPLETED);


        Page<Payment> paymentPage = new PageImpl<>(Collections.singletonList(payment));
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paymentPage);


        when(paymentMapper.toOverviewDto(any(Payment.class))).thenReturn(new PaymentOverviewDto());

        // Act
        Page<PaymentOverviewDto> result = paymentService.getPayments(
                token, null, null, null, null, null, accountNumber, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(paymentRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }


}