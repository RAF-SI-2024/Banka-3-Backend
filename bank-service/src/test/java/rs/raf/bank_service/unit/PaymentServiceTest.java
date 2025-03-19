package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.exceptions.CardNotFoundException;
import rs.raf.bank_service.exceptions.PaymentNotFoundException;
import rs.raf.bank_service.exceptions.ReceiverAccountNotFoundException;
import rs.raf.bank_service.exceptions.SenderAccountNotFoundException;
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
    private ObjectMapper objectMapper;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserClient userClient;

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

    @Test
    void getPaymentDetails_SuccessTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        Long paymentId = 100L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setClientId(clientId);

        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);
        when(paymentRepository.findByIdAndClientId(paymentId, clientId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDetailsDto(payment)).thenReturn(new PaymentDetailsDto());

        // Act
        PaymentDetailsDto result = paymentService.getPaymentDetails(token, paymentId);

        // Assert
        assertNotNull(result);
        verify(paymentRepository, times(1)).findByIdAndClientId(paymentId, clientId);
    }

    @Test
    void getPaymentDetails_NotFoundTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        Long paymentId = 100L;

        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);
        when(paymentRepository.findByIdAndClientId(paymentId, clientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentDetails(token, paymentId));
    }

    @Test
    void getPaymentsFilterByDatesTest() {
        // Arrange
        String token = "valid-token";
        Long clientId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now();

        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(clientId);

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setClientId(clientId);

        Page<Payment> paymentPage = new PageImpl<>(Collections.singletonList(payment));
        when(paymentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(paymentPage);
        when(paymentMapper.toOverviewDto(any(Payment.class))).thenReturn(new PaymentOverviewDto());

        // Act
        Page<PaymentOverviewDto> result = paymentService.getPayments(
                token, startDate, endDate, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(paymentRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }


    @Test
    void createTransferPendingConfirmation_SenderNotFoundTest() throws Exception {
        // Arrange
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("111111");
        transferDto.setReceiverAccountNumber("222222");
        transferDto.setAmount(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumberAndClientId(anyString(), anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SenderAccountNotFoundException.class, () ->
                paymentService.createTransferPendingConfirmation(transferDto, 1L));
    }

    @Test
    void createTransferPendingConfirmation_ReceiverNotFoundTest() throws Exception {
        // Arrange
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("111111");
        transferDto.setReceiverAccountNumber("222222");
        transferDto.setAmount(BigDecimal.valueOf(100));

        Account sender = new PersonalAccount();
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCurrency(new Currency("USD"));

        when(accountRepository.findByAccountNumberAndClientId(anyString(), anyLong()))
                .thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber(eq("222222")))
                .thenReturn(Optional.empty());


        // Act & Assert
        assertThrows(ReceiverAccountNotFoundException.class, () ->
                paymentService.createTransferPendingConfirmation(transferDto, 1L));
    }


    @Test
    void createPaymentBeforeConfirmation_SuccessTest() throws Exception {
        // Arrange
        Long clientId = 1L;
        CreatePaymentDto paymentDto = new CreatePaymentDto();
        paymentDto.setSenderAccountNumber("111111");
        paymentDto.setReceiverAccountNumber("222222");
        paymentDto.setAmount(BigDecimal.valueOf(100));
        paymentDto.setPaymentCode("289");
        paymentDto.setPurposeOfPayment("Invoice");
        paymentDto.setReferenceNumber("12345");

        Account sender = new PersonalAccount();
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCurrency(new Currency("USD"));

        Account receiver = new PersonalAccount();
        receiver.setBalance(BigDecimal.valueOf(500));
        receiver.setCurrency(new Currency("USD"));
        receiver.setClientId(2L);

        ClientDto clientDto = new ClientDto();
        clientDto.setFirstName("John");
        clientDto.setLastName("Doe");

        when(accountRepository.findByAccountNumberAndClientId(anyString(), eq(clientId))).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber(eq("222222"))).thenReturn(Optional.of(receiver));
        when(userClient.getClientById(clientId)).thenReturn(clientDto);
        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        // Act
        boolean result = paymentService.createPaymentBeforeConfirmation(paymentDto, clientId);

        // Assert
        assertTrue(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(userClient, times(1)).createVerificationRequest(any(CreateVerificationRequestDto.class));
    }

}