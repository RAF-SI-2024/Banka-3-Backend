package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.dto.PaymentDto;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.service.PaymentService;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Account senderAccount;
    private Account receiverAccount;
    private PaymentDto paymentDto;
    private TransferDto transferDto;

    @BeforeEach
    public void setUp() {
        senderAccount = new PersonalAccount();
        senderAccount.setAccountNumber("123456789012345678");
        senderAccount.setBalance(BigDecimal.valueOf(1000));
        Currency currency = new Currency();
        currency.setCode("RSD");
        currency.setActive(true);
        senderAccount.setCurrency(currency);

        // Kreiramo primaoca
        receiverAccount = new PersonalAccount();
        receiverAccount.setAccountNumber("987654321098765432");
        receiverAccount.setBalance(BigDecimal.valueOf(500));
        receiverAccount.setCurrency(currency);

        // Kreiramo DTO za uplatu
        paymentDto = new PaymentDto();
        paymentDto.setSenderAccountNumber(senderAccount.getAccountNumber());
        paymentDto.setReceiverAccountNumber(receiverAccount.getAccountNumber());
        paymentDto.setAmount(BigDecimal.valueOf(100));
        paymentDto.setPaymentCode("289");
        paymentDto.setPurposeOfPayment("Payment for service");

        // Kreiramo DTO za transfer
        transferDto = new TransferDto();
        transferDto.setSenderAccountNumber(senderAccount.getAccountNumber());
        transferDto.setReceiverAccountNumber(receiverAccount.getAccountNumber());
        transferDto.setAmount(BigDecimal.valueOf(200));
    }

    @Test
    public void testTransferFunds_Success() {
        // Simulacija vraćanja računa po broju računa
        when(accountRepository.findByAccountNumber(senderAccount.getAccountNumber()))
                .thenReturn(Arrays.asList(senderAccount));
        when(accountRepository.findByAccountNumber(receiverAccount.getAccountNumber()))
                .thenReturn(Arrays.asList(receiverAccount));

        // Pozivamo metodu transferFunds
        paymentService.transferFunds(transferDto);

        // Proveravamo da li su stanja računa ažurirana
        assertEquals(BigDecimal.valueOf(800), senderAccount.getBalance());  // Pošiljalac je smanjen za 200
        assertEquals(BigDecimal.valueOf(700), receiverAccount.getBalance());  // Primaoc je uvećan za 200

        // Verifikujemo da su oba računa sačuvana u bazi
        verify(accountRepository).save(senderAccount);
        verify(accountRepository).save(receiverAccount);
    }

    /**
    @Test
    public void testTransferFunds_InsufficientFunds() {
        transferDto.setAmount(BigDecimal.valueOf(2000)); // Insufficient amount

        when(accountRepository.findByAccountNumber(senderAccount.getAccountNumber()))
                .thenReturn(Arrays.asList(senderAccount));
        when(accountRepository.findByAccountNumber(receiverAccount.getAccountNumber()))
                .thenReturn(Arrays.asList(receiverAccount));

        // Testiramo grešku sa nedovoljno sredstava
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> paymentService.transferFunds(transferDto));
        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }
    */
    @Test
    public void testMakePayment_Success() {
        when(accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber()))
                .thenReturn(Arrays.asList(senderAccount));
        when(accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber()))
                .thenReturn(Arrays.asList(receiverAccount));

        boolean result = paymentService.makePayment(paymentDto);

        assertTrue(result);
        assertEquals(BigDecimal.valueOf(900), senderAccount.getBalance());
        assertEquals(BigDecimal.valueOf(600), receiverAccount.getBalance());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

/**
    @Test
    public void testMakePayment_MissingPaymentCode() {
        paymentDto.setPaymentCode(null);

        // Testiramo grešku ako nije unet kod uplate
        PaymentCodeNotProvidedException exception = assertThrows(PaymentCodeNotProvidedException.class,
                () -> paymentService.makePayment(paymentDto));
        assertTrue(exception.getMessage().contains("Payment code is required"));
    }

    @Test
    public void testMakePayment_InsufficientFunds() {
        paymentDto.setAmount(BigDecimal.valueOf(2000)); // Insufficient amount

        when(accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber()))
                .thenReturn(Arrays.asList(senderAccount));
        when(accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber()))
                .thenReturn(Arrays.asList(receiverAccount));

        // Testiramo grešku sa nedovoljno sredstava
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> paymentService.makePayment(paymentDto));
        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }
*/
}
