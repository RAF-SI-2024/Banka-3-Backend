package rs.raf.bank_service.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.domain.mapper.PaymentMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {

    @InjectMocks private PaymentService paymentService;

    @Mock private AccountRepository accountRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CompanyAccountRepository companyAccountRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private UserClient userClient;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ExchangeRateService exchangeRateService;
    @Mock private ObjectMapper objectMapper;

    private final String token = "Bearer token";
    private final Long userId = 1L;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(jwtTokenUtil.getUserIdFromAuthHeader(token)).thenReturn(userId);
    }

    private Currency rsdCurrency() {
        Currency c = new Currency();
        c.setCode("RSD");
        return c;
    }

    private ClientDto mockClient() {
        ClientDto client = new ClientDto();
        client.setFirstName("Marko");
        client.setLastName("Marković");
        return client;
    }

    @Test
    public void testGetPayments_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Account acc = new PersonalAccount();
        Card card = new Card();
        Payment payment = new Payment();
        PaymentOverviewDto dto = new PaymentOverviewDto();

        when(accountRepository.findByAccountNumber("ACC")).thenReturn(Optional.of(acc));
        when(cardRepository.findByCardNumber("CARD")).thenReturn(Optional.of(card));
        when(paymentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(payment)));
        when(paymentMapper.toOverviewDto(any())).thenReturn(dto);

        Page<PaymentOverviewDto> page = paymentService.getPayments(
                token, null, null, null, null, null, "ACC", "CARD", pageable);

        assertEquals(1, page.getContent().size());
    }

    @Test
    public void testGetPaymentDetails_success() {
        Payment payment = new Payment();
        PaymentDetailsDto dto = new PaymentDetailsDto();
        when(paymentRepository.findByIdAndClientId(1L, userId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDetailsDto(payment)).thenReturn(dto);

        assertEquals(dto, paymentService.getPaymentDetails(token, 1L));
    }

    @Test
    public void testConfirmPayment_setsStatusCompleted() {
        Currency rsd = new Currency();
        rsd.setCode("RSD");

        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC001");
        sender.setCurrency(rsd);
        sender.setClientId(1L);
        sender.setBalance(BigDecimal.valueOf(1000)); // ✅ DODATO
        sender.setAvailableBalance(BigDecimal.valueOf(800)); // ✅ DODATO

        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC002");
        receiver.setCurrency(rsd);
        receiver.setBalance(BigDecimal.valueOf(300)); // ✅ DODATO
        receiver.setAvailableBalance(BigDecimal.valueOf(300)); // ✅ DODATO

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver("ACC002");
        payment.setClientId(1L);
        payment.setAmount(BigDecimal.valueOf(200));
        payment.setOutAmount(BigDecimal.valueOf(200)); // ✅ OBAVEZNO jer se koristi kod currency checka

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(receiver));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> paymentService.confirmPayment(1L));
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }

    @Test
    public void testRejectPayment_setsStatusCanceled() {
        Currency rsd = new Currency();
        rsd.setCode("RSD");

        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC01");
        sender.setAvailableBalance(BigDecimal.valueOf(1000));
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setCurrency(rsd);
        sender.setClientId(1L);

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setSenderAccount(sender);
        payment.setAmount(BigDecimal.valueOf(200));
        payment.setClientId(1L);

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("ACC01")).thenReturn(Optional.of(sender));

        assertDoesNotThrow(() -> paymentService.rejectPayment(5L));
        assertEquals(PaymentStatus.CANCELED, payment.getStatus());
    }

    @Test
    public void testGetExchangeProfit() {
        when(paymentRepository.getBankProfitFromExchange()).thenReturn(BigDecimal.valueOf(11));

        BigDecimal profit = assertDoesNotThrow(() -> paymentService.getExchangeProfit());
        assertEquals(BigDecimal.valueOf(11), profit);
    }


    @Test
    public void testCreateTransferAndVerificationRequest_success() throws JsonProcessingException {
        Long clientId = 2L;

        // Setup currency
        Currency rsdCurrency = new Currency();
        rsdCurrency.setCode("RSD");

        // Setup sender account
        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC001");
        sender.setAvailableBalance(BigDecimal.valueOf(1000));
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setClientId(clientId);
        sender.setCurrency(rsdCurrency);

        // Setup receiver account
        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC002");
        receiver.setClientId(clientId);
        receiver.setCurrency(rsdCurrency);

        // DTO for transfer
        TransferDto dto = new TransferDto();
        dto.setSenderAccountNumber("ACC001");
        dto.setReceiverAccountNumber("ACC002");
        dto.setAmount(BigDecimal.valueOf(500));

        // CurrencyDto & ExchangeRate mock
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setCode("RSD");

        ExchangeRateDto rateDto = new ExchangeRateDto(currencyDto, currencyDto, BigDecimal.ONE, BigDecimal.ONE);

        // Payment object expected to be saved
        Payment payment = new Payment();
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());
        payment.setAmount(dto.getAmount());
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setClientId(clientId);
        payment.setDate(LocalDateTime.now());
        payment.setOutAmount(BigDecimal.valueOf(500));
        payment.setExchangeProfit(BigDecimal.ZERO);

        // Mock repositories and services
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(receiver));
        when(exchangeRateService.getExchangeRate("RSD", "RSD")).thenReturn(rateDto);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(userClient.getClientById(clientId)).thenReturn(new ClientDto(clientId, "John", "Doe", "john@example.com"));
        doNothing().when(userClient).createVerificationRequest(any());

        // Mock PaymentMapper
        PaymentDto paymentDto = new PaymentDto();
        when(paymentMapper.toPaymentDto(any(Payment.class), eq("Receiver"))).thenReturn(paymentDto);

        // Execute & verify
        PaymentDto result = assertDoesNotThrow(() -> paymentService.createTransferAndVerificationRequest(dto, clientId));
        assertEquals(paymentDto, result);

        verify(accountRepository, times(1)).findByAccountNumber("ACC001");
        verify(accountRepository, times(2)).findByAccountNumber("ACC002");
        verify(exchangeRateService, atMost(1)).getExchangeRate(any(), any());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(userClient, times(1)).getClientById(clientId);
        verify(userClient, times(1)).createVerificationRequest(any());
        verify(paymentMapper, times(1)).toPaymentDto(any(Payment.class), eq("Receiver"));
    }

    @Test
    public void testCreateAndExecuteSystemPayment_success() {
        Currency rsd = new Currency();
        rsd.setCode("RSD");

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setSenderAccountNumber("ACC1");
        dto.setReceiverAccountNumber("ACC2");
        dto.setAmount(BigDecimal.valueOf(75));
        dto.setPaymentCode("97");
        dto.setPurposeOfPayment("Kupovina");
        dto.setReferenceNumber("REF123");
        dto.setCallbackId(42L);

        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC1");
        sender.setAvailableBalance(BigDecimal.valueOf(500));
        sender.setBalance(BigDecimal.valueOf(500));
        sender.setCurrency(rsd);
        sender.setClientId(userId);

        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC2");
        receiver.setCurrency(rsd);
        receiver.setBalance(BigDecimal.valueOf(0)); // ✔️ DA SE IZBEGNE NPE
        receiver.setAvailableBalance(BigDecimal.valueOf(0));


        Payment savedPayment = new Payment();
        savedPayment.setId(123L);
        savedPayment.setSenderAccount(sender);
        savedPayment.setAccountNumberReceiver("ACC2");
        savedPayment.setClientId(userId);
        savedPayment.setStatus(PaymentStatus.PENDING_CONFIRMATION); // ✅ OBAVEZNO
        savedPayment.setAmount(dto.getAmount());
        savedPayment.setOutAmount(dto.getAmount());

        when(accountRepository.findByAccountNumber("ACC1")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC2")).thenReturn(Optional.of(receiver));
        when(userClient.getClientById(userId)).thenReturn(new ClientDto());
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(123L); // dodeli ID kako bi confirmPayment mogao da ga koristi
            return saved;
        });
        when(paymentRepository.findById(123L)).thenReturn(Optional.of(savedPayment));

        assertDoesNotThrow(() -> paymentService.createAndExecuteSystemPayment(dto, userId));
    }

    @Test
    public void testCreatePaymentAndVerificationRequest_success() {
        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setSenderAccountNumber("ACC123");
        dto.setReceiverAccountNumber("ACC456");
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setPaymentCode("289");
        dto.setPurposeOfPayment("Plaćanje usluga");
        dto.setReferenceNumber("123-456");
        dto.setCallbackId(1L);

        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC123");
        sender.setAvailableBalance(BigDecimal.valueOf(200));
        sender.setBalance(BigDecimal.valueOf(200));
        sender.setCurrency(rsdCurrency());
        sender.setClientId(userId);

        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC456");
        receiver.setCurrency(rsdCurrency());

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC456")).thenReturn(Optional.of(receiver));
        when(userClient.getClientById(userId)).thenReturn(mockClient());
        doNothing().when(userClient).createVerificationRequest(any());
        when(paymentRepository.save(any())).thenReturn(new Payment());

        assertDoesNotThrow(() -> paymentService.createPaymentAndVerificationRequest(dto, userId));
    }

    @Test
    public void testValidatePaymentData_missingPaymentCode_throwsException() {
        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setPaymentCode(null);
        dto.setPurposeOfPayment("Uplata");
        dto.setReceiverAccountNumber("ACC002");
        dto.setSenderAccountNumber("ACC001");
        dto.setAmount(BigDecimal.valueOf(100));

        assertThrows(PaymentCodeNotProvidedException.class, () ->
                paymentService.createPaymentAndVerificationRequest(dto, userId)
        );
    }

    @Test
    public void testValidatePaymentData_missingPurpose_throwsException() {
        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setPaymentCode("123");
        dto.setPurposeOfPayment("");
        dto.setReceiverAccountNumber("ACC002");
        dto.setSenderAccountNumber("ACC001");
        dto.setAmount(BigDecimal.valueOf(100));

        assertThrows(PurposeOfPaymentNotProvidedException.class, () ->
                paymentService.createPaymentAndVerificationRequest(dto, userId)
        );
    }

    @Test
    public void testCreatePayment_differentCurrencies_triggersExchangeConversion() throws JsonProcessingException {
        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setSenderAccountNumber("ACC1");
        dto.setReceiverAccountNumber("ACC2");
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setPaymentCode("99");
        dto.setPurposeOfPayment("Pretplata");
        dto.setReferenceNumber("REF001");

        Currency eur = new Currency();
        eur.setCode("EUR");

        Currency rsd = new Currency();
        rsd.setCode("RSD");

        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC1");
        sender.setAvailableBalance(BigDecimal.valueOf(500));
        sender.setBalance(BigDecimal.valueOf(500));
        sender.setCurrency(eur);
        sender.setClientId(userId);

        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC2");
        receiver.setCurrency(rsd);
        receiver.setBalance(BigDecimal.ZERO); // ✔️ važno za receiver.getBalance().add(...)
        receiver.setAvailableBalance(BigDecimal.ZERO);
        receiver.setClientId(999L);

        ExchangeRateDto rateDto = new ExchangeRateDto(
                new CurrencyDto("EUR", "EURO", "EUR"),
                new CurrencyDto("RSD", "DINAR", "RSD"),
                BigDecimal.valueOf(117.5), // exchange rate
                BigDecimal.valueOf(117.0)  // sell rate
        );

        // Expected output DTO
        PaymentDto resultDto = new PaymentDto();

        // Mockovi
        when(accountRepository.findByAccountNumber("ACC1")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC2")).thenReturn(Optional.of(receiver));
        when(userClient.getClientById(userId)).thenReturn(new ClientDto(1L,"Ana", "Anić", "ana@mail.com"));
        when(exchangeRateService.getExchangeRate("EUR", "RSD")).thenReturn(rateDto);
        when(exchangeRateService.convert(any(ConvertDto.class))).thenReturn(BigDecimal.valueOf(50));
        when(paymentRepository.save(any())).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(123L);
            return p;
        });
        doNothing().when(userClient).createVerificationRequest(any());
        when(paymentMapper.toPaymentDto(any(Payment.class), eq("Receiver"))).thenReturn(resultDto);

        // Poziv
        PaymentDto result = assertDoesNotThrow(() ->
                paymentService.createPaymentAndVerificationRequest(dto, userId)
        );

        // Provera
        assertEquals(resultDto, result);
    }

    @Test
    public void testConfirmPayment_processDifferentCurrencyPayment_success() {
        // Setup valuta
        Currency eur = new Currency();
        eur.setCode("EUR");

        Currency rsd = new Currency();
        rsd.setCode("RSD");

        // Sender (EUR)
        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC001");
        sender.setCurrency(eur);
        sender.setBalance(BigDecimal.valueOf(500));
        sender.setAvailableBalance(BigDecimal.valueOf(400));
        sender.setClientId(userId);

        // Receiver (RSD)
        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC002");
        receiver.setCurrency(rsd);
        receiver.setBalance(BigDecimal.ZERO);
        receiver.setAvailableBalance(BigDecimal.ZERO);
        receiver.setClientId(999L);

        // Bank account EUR (prima iznos od korisnika)
        CompanyAccount bankFrom = new CompanyAccount();
        bankFrom.setCurrency(eur);
        bankFrom.setBalance(BigDecimal.valueOf(1000));
        bankFrom.setAvailableBalance(BigDecimal.valueOf(1000));

        // Bank account RSD (daje konvertovani iznos)
        CompanyAccount bankTo = new CompanyAccount();
        bankTo.setCurrency(rsd);
        bankTo.setBalance(BigDecimal.valueOf(2000));
        bankTo.setAvailableBalance(BigDecimal.valueOf(2000));

        // Payment entitet
        Payment payment = new Payment();
        payment.setId(55L);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver("ACC002");
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setOutAmount(BigDecimal.valueOf(11700)); // konvertovani iznos
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);

        // Mockovi
        when(paymentRepository.findById(55L)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(receiver));
        when(accountRepository.findFirstByCurrencyAndCompanyId(eur, 1L)).thenReturn(Optional.of(bankFrom));
        when(accountRepository.findFirstByCurrencyAndCompanyId(rsd, 1L)).thenReturn(Optional.of(bankTo));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(paymentMapper.toDetailsDto(any())).thenReturn(new PaymentDetailsDto());

        // Poziv
        PaymentDetailsDto result = assertDoesNotThrow(() -> paymentService.confirmPayment(55L));

        // Provera
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }

    @Test
    public void testConfirmPayment_processSameCurrencyPayment_success() {
        // Setup valuta
        Currency rsd = new Currency();
        rsd.setCode("RSD");

        // Sender (RSD)
        PersonalAccount sender = new PersonalAccount();
        sender.setAccountNumber("ACC001");
        sender.setCurrency(rsd);
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setAvailableBalance(BigDecimal.valueOf(800));
        sender.setClientId(userId);

        // Receiver (RSD)
        PersonalAccount receiver = new PersonalAccount();
        receiver.setAccountNumber("ACC002");
        receiver.setCurrency(rsd);
        receiver.setBalance(BigDecimal.valueOf(500));
        receiver.setAvailableBalance(BigDecimal.valueOf(500));
        receiver.setClientId(999L);

        // Payment entitet
        Payment payment = new Payment();
        payment.setId(66L);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver("ACC002");
        payment.setAmount(BigDecimal.valueOf(200));
        payment.setOutAmount(BigDecimal.valueOf(200));
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);

        // Mockovi
        when(paymentRepository.findById(66L)).thenReturn(Optional.of(payment));
        when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(receiver));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(paymentMapper.toDetailsDto(any())).thenReturn(new PaymentDetailsDto());

        // Poziv
        PaymentDetailsDto result = assertDoesNotThrow(() -> paymentService.confirmPayment(66L));

        // Provera
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }


}
