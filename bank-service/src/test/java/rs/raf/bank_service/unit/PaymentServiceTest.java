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
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.domain.mapper.PaymentMapper;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private AccountService accountService;

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
        paymentDto.setRecieverName("Jane Doe");

        Account sender = new PersonalAccount();
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCurrency(new Currency("USD"));
        sender.setClientId(clientId);

        Account receiver = new PersonalAccount();
        receiver.setBalance(BigDecimal.valueOf(500));
        receiver.setCurrency(new Currency("USD"));
        receiver.setClientId(2L);

        ClientDto clientDto = new ClientDto();
        clientDto.setFirstName("John");
        clientDto.setLastName("Doe");


        when(accountService.getBankCode(eq("222222"))).thenReturn("111");
        // Simuliramo da accountRepository vraća sender i receiver
        when(accountRepository.findByAccountNumberAndClientId(eq("111111"), eq(clientId)))
                .thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber(eq("222222")))
                .thenReturn(Optional.of(receiver));

        // Simuliramo da userClient vraća klijenta
        when(userClient.getClientById(clientId)).thenReturn(clientDto);

        // Simulacija dodeljivanja ID-a Payment objektu
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment savedPayment = invocation.getArgument(0);
            savedPayment.setId(1L); // Simuliramo da baza postavlja ID
            return savedPayment;
        });

        // Simulacija mapper-a koji konvertuje u DTO
        when(paymentMapper.toPaymentDto(any(Payment.class), anyString())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            return new PaymentDto(payment.getId(), payment.getAmount(), payment.getPurposeOfPayment(),
                    payment.getSenderAccount().getAccountNumber(), payment.getAccountNumberReceiver(),
                    "", payment.getPaymentCode(), payment.getPaymentCode());
        });

        doNothing().when(userClient).createVerificationRequest(any(CreateVerificationRequestDto.class));

        // Act
        PaymentDto result = paymentService.createPaymentBeforeConfirmation(paymentDto, clientId);

        // Assert
        assertNotNull(result, "PaymentDto should not be null");
        assertNotNull(result.getId(), "PaymentDto ID should not be null");
        assertEquals(BigDecimal.valueOf(100), result.getAmount(), "Amount should match input");

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(userClient, times(1)).createVerificationRequest(any(CreateVerificationRequestDto.class));
    }



    @Test
    void confirmTransferAndExecute_SameCurrency_Success() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setAmount(BigDecimal.valueOf(100));

        Account sender = new PersonalAccount();
        sender.setAccountNumber("1");
        sender.setBalance(BigDecimal.valueOf(500));
        Currency usd = new Currency("USD");
        sender.setCurrency(usd);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("2");
        receiver.setBalance(BigDecimal.valueOf(200));
        receiver.setCurrency(usd);

        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
                .thenReturn(Optional.of(receiver));

        boolean result = paymentService.confirmTransferAndExecute(paymentId);

        assertTrue(result);
        assertEquals(BigDecimal.valueOf(400), sender.getBalance());
        assertEquals(BigDecimal.valueOf(300), receiver.getBalance());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(receiver);
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void confirmTransferAndExecute_DifferentCurrencies_Success() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setAmount(BigDecimal.valueOf(100));

        Currency usd = new Currency("USD");
        Currency eur = new Currency("EUR");

        Account sender = new PersonalAccount();
        sender.setAccountNumber("1");
        sender.setBalance(BigDecimal.valueOf(500));
        sender.setCurrency(usd);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("2");
        receiver.setBalance(BigDecimal.valueOf(200));
        receiver.setCurrency(eur);

        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());

        CompanyAccount bankAccountFrom = new CompanyAccount();
        bankAccountFrom.setAccountNumber("3");
        bankAccountFrom.setCurrency(usd);
        bankAccountFrom.setCompanyId(1L);
        bankAccountFrom.setBalance(BigDecimal.valueOf(1000));

        CompanyAccount bankAccountTo = new CompanyAccount();
        bankAccountTo.setAccountNumber("4");
        bankAccountTo.setCurrency(eur);
        bankAccountTo.setCompanyId(1L);
        bankAccountTo.setBalance(BigDecimal.valueOf(2000));

        ExchangeRateDto exchangeRateDto = new ExchangeRateDto();
        exchangeRateDto.setExchangeRate(BigDecimal.valueOf(0.85));
        BigDecimal convertedAmount = payment.getAmount().multiply(exchangeRateDto.getExchangeRate());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
                .thenReturn(Optional.of(receiver));
        when(accountRepository.findFirstByCurrencyAndCompanyId(usd, 1L))
                .thenReturn(Optional.of(bankAccountFrom));
        when(accountRepository.findFirstByCurrencyAndCompanyId(eur, 1L))
                .thenReturn(Optional.of(bankAccountTo));
        when(exchangeRateService.getExchangeRate("USD", "EUR"))
                .thenReturn(exchangeRateDto);

        boolean result = paymentService.confirmTransferAndExecute(paymentId);

        assertTrue(result);
        assertEquals(BigDecimal.valueOf(400), sender.getBalance());
        assertEquals(BigDecimal.valueOf(1100), bankAccountFrom.getBalance());
//        assertEquals(BigDecimal.valueOf(1915.0), bankAccountTo.getBalance());
//        assertEquals(BigDecimal.valueOf(285), receiver.getBalance());
        assertEquals(convertedAmount, payment.getOutAmount());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());

        verify(accountRepository, times(2)).save(sender);
        verify(accountRepository, times(1)).save(bankAccountFrom);
        verify(accountRepository, times(1)).save(bankAccountTo);
        verify(accountRepository, times(1)).save(receiver);
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void confirmTransferAndExecute_PaymentNotFound_ThrowsException() {
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.confirmTransferAndExecute(paymentId));
    }

    @Test
    void confirmTransferAndExecute_ReceiverAccountNotFound_ThrowsException() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setAccountNumberReceiver("RECEIVER123");
        Account sender = new PersonalAccount();
        sender.setCurrency(new Currency("USD"));
        payment.setSenderAccount(sender);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("RECEIVER123")).thenReturn(Optional.empty());

        assertThrows(ReceiverAccountNotFoundException.class, () -> paymentService.confirmTransferAndExecute(paymentId));
    }

    @Test
    void confirmTransferAndExecute_BankAccountFromNotFound_ThrowsException() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setAccountNumberReceiver("RECEIVER123");
        Account sender = new PersonalAccount();
        Currency usd = new Currency("USD");
        sender.setCurrency(usd);
        payment.setSenderAccount(sender);

        Account receiver = new PersonalAccount();
        Currency eur = new Currency("EUR");
        receiver.setCurrency(eur);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("RECEIVER123")).thenReturn(Optional.of(receiver));
        when(accountRepository.findFirstByCurrencyAndCompanyId(usd, 1L)).thenReturn(Optional.empty());

        assertThrows(BankAccountNotFoundException.class, () -> paymentService.confirmTransferAndExecute(paymentId));
    }

    @Test
    void confirmPayment_SameCurrency_Success() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setAmount(BigDecimal.valueOf(100));

        Account sender = new PersonalAccount();
        sender.setAccountNumber("1");
        sender.setBalance(BigDecimal.valueOf(500));
        Currency usd = new Currency("USD");
        sender.setCurrency(usd);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("2");
        receiver.setBalance(BigDecimal.valueOf(200));
        receiver.setCurrency(usd);

        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
                .thenReturn(Optional.of(receiver));

        paymentService.confirmPayment(paymentId);

        assertEquals(BigDecimal.valueOf(400), sender.getBalance());
        assertEquals(BigDecimal.valueOf(300), receiver.getBalance());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        verify(accountRepository, times(1)).save(sender);
        verify(accountRepository, times(1)).save(receiver);
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void confirmPayment_DifferentCurrencies_Success() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setAmount(BigDecimal.valueOf(100));

        Currency usd = new Currency("USD");
        Currency eur = new Currency("EUR");

        Account sender = new PersonalAccount();
        sender.setAccountNumber("1");
        sender.setBalance(BigDecimal.valueOf(500));
        sender.setCurrency(usd);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("2");
        receiver.setBalance(BigDecimal.valueOf(200));
        receiver.setCurrency(eur);

        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());

        CompanyAccount bankAccountFrom = new CompanyAccount();
        bankAccountFrom.setAccountNumber("3");
        bankAccountFrom.setCurrency(usd);
        bankAccountFrom.setCompanyId(1L);
        bankAccountFrom.setBalance(BigDecimal.valueOf(1000));

        CompanyAccount bankAccountTo = new CompanyAccount();
        bankAccountTo.setAccountNumber("4");
        bankAccountTo.setCurrency(eur);
        bankAccountTo.setCompanyId(1L);
        bankAccountTo.setBalance(BigDecimal.valueOf(2000));

        ExchangeRateDto exchangeRateDto = new ExchangeRateDto();
        exchangeRateDto.setExchangeRate(BigDecimal.valueOf(0.85));
        BigDecimal convertedAmount = payment.getAmount().multiply(exchangeRateDto.getExchangeRate());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber(receiver.getAccountNumber()))
                .thenReturn(Optional.of(receiver));
        when(accountRepository.findFirstByCurrencyAndCompanyId(usd, 1L))
                .thenReturn(Optional.of(bankAccountFrom));
        when(accountRepository.findFirstByCurrencyAndCompanyId(eur, 1L))
                .thenReturn(Optional.of(bankAccountTo));
        when(exchangeRateService.getExchangeRate("USD", "EUR"))
                .thenReturn(exchangeRateDto);

        paymentService.confirmPayment(paymentId);

        assertEquals(BigDecimal.valueOf(400), sender.getBalance());
        assertEquals(BigDecimal.valueOf(1100), bankAccountFrom.getBalance());
//        assertEquals(BigDecimal.valueOf(1915), bankAccountTo.getBalance());
//        assertEquals(BigDecimal.valueOf(285), receiver.getBalance());
        assertEquals(convertedAmount, payment.getOutAmount());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());

        verify(accountRepository, times(2)).save(sender);
        verify(accountRepository, times(1)).save(bankAccountFrom);
        verify(accountRepository, times(1)).save(bankAccountTo);
        verify(accountRepository, times(1)).save(receiver);
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void confirmPayment_PaymentNotFound_ThrowsException() {
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.confirmPayment(paymentId));
    }

    @Test
    void confirmPayment_ReceiverAccountNotFound_ThrowsException() {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setAccountNumberReceiver("RECEIVER123");
        Account sender = new PersonalAccount();
        sender.setCurrency(new Currency("USD"));
        payment.setSenderAccount(sender);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("RECEIVER123")).thenReturn(Optional.empty());

        assertThrows(ReceiverAccountNotFoundException.class, () -> paymentService.confirmPayment(paymentId));
    }

    @Test
    void createTransferPendingConfirmation_Success_SameCurrency() throws Exception {
        Long clientId = 1L;
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("111111");
        transferDto.setReceiverAccountNumber("222222");
        transferDto.setAmount(BigDecimal.valueOf(100));

        Currency currency = new Currency("RSD");
        Account sender = new PersonalAccount();
        sender.setAccountNumber("111111");
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCurrency(currency);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("222222");
        receiver.setCurrency(currency);
        receiver.setClientId(2L);

        when(accountRepository.findByAccountNumberAndClientId("111111", clientId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("222222")).thenReturn(Optional.of(receiver));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        boolean result = paymentService.createTransferPendingConfirmation(transferDto, clientId);

        assertTrue(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(userClient, times(1)).createVerificationRequest(any(CreateVerificationRequestDto.class));
    }

    @Test
    void createTransferPendingConfirmation_Success_DifferentCurrency() throws Exception {
        Long clientId = 1L;
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("111111");
        transferDto.setReceiverAccountNumber("222222");
        transferDto.setAmount(BigDecimal.valueOf(100));

        Currency usd = new Currency("USD");
        Currency eur = new Currency("EUR");

        Account sender = new PersonalAccount();
        sender.setAccountNumber("111111");
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCurrency(usd);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("222222");
        receiver.setCurrency(eur);
        receiver.setClientId(2L);

        ExchangeRateDto exchangeRateDto = new ExchangeRateDto();
        exchangeRateDto.setSellRate(BigDecimal.valueOf(0.9));

        when(accountRepository.findByAccountNumberAndClientId("111111", clientId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("222222")).thenReturn(Optional.of(receiver));
        when(exchangeRateService.getExchangeRate("USD", "EUR")).thenReturn(exchangeRateDto);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("mocked-json");

        boolean result = paymentService.createTransferPendingConfirmation(transferDto, clientId);

        assertTrue(result);
        verify(exchangeRateService).getExchangeRate("USD", "EUR");
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(userClient, times(1)).createVerificationRequest(any(CreateVerificationRequestDto.class));
    }

    @Test
    void createTransferPendingConfirmation_InsufficientFunds_ThrowsException() {
        TransferDto transferDto = new TransferDto();
        transferDto.setSenderAccountNumber("111111");
        transferDto.setReceiverAccountNumber("222222");
        transferDto.setAmount(BigDecimal.valueOf(1000));

        Currency currency = new Currency("RSD");
        Account sender = new PersonalAccount();
        sender.setAccountNumber("111111");
        sender.setBalance(BigDecimal.valueOf(500));
        sender.setCurrency(currency);

        Account receiver = new PersonalAccount();
        receiver.setAccountNumber("222222");
        receiver.setCurrency(currency);

        when(accountRepository.findByAccountNumberAndClientId("111111", 1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("222222")).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientFundsException.class, () ->
                paymentService.createTransferPendingConfirmation(transferDto, 1L));
    }




}