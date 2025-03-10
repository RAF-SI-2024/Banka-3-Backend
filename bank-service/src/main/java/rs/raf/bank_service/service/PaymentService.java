package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.CurrencyType;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.domain.enums.VerificationType;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.mapper.PaymentMapper;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.specification.PaymentSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private PaymentRepository paymentRepository;
    private CardRepository cardRepository;
    private final UserClient userClient;
    private final PaymentMapper paymentMapper;
    private final ExchangeRateService exchangeRateService;

    public boolean createTransferPendingConfirmation(TransferDto transferDto, Long clientId) {
        // Preuzimanje računa za sender i receiver
        Account sender = accountRepository.findByAccountNumber(transferDto.getSenderAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new SenderAccountNotFoundException(transferDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(transferDto.getReceiverAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(transferDto.getReceiverAccountNumber()));

        // Provera da li sender ima dovoljno sredstava
        if (sender.getBalance().compareTo(transferDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), transferDto.getAmount());
        }

        // Provera da li su računi isti tip valute
        if (!(sender.getCurrency().equals(receiver.getCurrency()))) {
            throw new NotSameCurrencyForTransferException(sender.getCurrency().toString(), receiver.getCurrency().toString());
        }

        // Kreiranje Payment entiteta za transfer
        Payment payment = new Payment();
        payment.setClientId(clientId);  // Dodajemo Client ID
        payment.setSenderAccount(sender);  // Sender račun
        payment.setAmount(transferDto.getAmount());  // Iznos
        payment.setAccountNumberReceiver(transferDto.getReceiverAccountNumber());  // Primalac (receiver)
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);  // Status je "na čekanju"
        payment.setDate(LocalDateTime.now());  // Datum transakcije

        // Postavi receiverClientId samo ako je receiver u našoj banci
        payment.setReceiverClientId(receiver.getClientId());  // Postavljamo receiverClientId

        paymentRepository.save(payment);

        // Kreiraj PaymentVerificationRequestDto i pozovi UserClient da kreira verificationRequest
        CreateVerificationRequestDto paymentVerificationRequestDto = new CreateVerificationRequestDto(clientId, payment.getId(), VerificationType.TRANSFER);
        userClient.createVerificationRequest(paymentVerificationRequestDto);

        return true;
    }

    public boolean confirmTransferAndExecute(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Account sender = payment.getSenderAccount();
        Account receiver = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver())
                .orElseThrow(() -> new ReceiverAccountNotFoundException(payment.getAccountNumberReceiver()));

        //  Provera da li sender ima dovoljno sredstava
        if (sender.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), payment.getAmount());
        }

        //  Oduzimanje novca sa sender računa
        sender.setBalance(sender.getBalance().subtract(payment.getAmount()));
        accountRepository.save(sender);

        //  Pronalazak bankovnog računa u osnovnoj valuti (RSD)
        Account bankAccount = accountRepository.findByAccountNumber("333000156732897612")
                .orElseThrow(() -> new BankAccountNotFoundException("Bank RSD Account not found"));

        //  Konverzija u RSD (ako je potrebno)
        BigDecimal rsdAmount = payment.getAmount();
        if (!sender.getCurrency().getCode().equals("RSD")) {
            rsdAmount = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), "RSD")
                    .multiply(payment.getAmount());
        }

        // Dodavanje novca na bankovni račun
        bankAccount.setBalance(bankAccount.getBalance().add(rsdAmount));
        accountRepository.save(bankAccount);

        //  Konverzija iz RSD u valutu ciljnog računa
        BigDecimal convertedAmount = rsdAmount;
        if (!receiver.getCurrency().getCode().equals("RSD")) {
            convertedAmount = exchangeRateService.getExchangeRate("RSD", receiver.getCurrency().getCode())
                    .multiply(rsdAmount);
        }

        //  Prebacivanje novca sa banke na ciljni račun
        bankAccount.setBalance(bankAccount.getBalance().subtract(convertedAmount));
        receiver.setBalance(receiver.getBalance().add(convertedAmount));

        accountRepository.save(bankAccount);
        accountRepository.save(receiver);

        //  Ažuriranje statusa transakcije na "COMPLETED"
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        return true;
    }

    public boolean createPaymentBeforeConfirmation(CreatePaymentDto paymentDto, Long clientId) {
        if (paymentDto.getPaymentCode() == null || paymentDto.getPaymentCode().isEmpty()) {
            throw new PaymentCodeNotProvidedException();
        }

        if (paymentDto.getPurposeOfPayment() == null || paymentDto.getPurposeOfPayment().isEmpty()) {
            throw new PurposeOfPaymentNotProvidedException();
        }

        // Preuzimanje sender računa
        Account sender = accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new SenderAccountNotFoundException(paymentDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(paymentDto.getReceiverAccountNumber()));


        // Provera valute
        if (!(sender.getCurrency().getCode().equals(CurrencyType.RSD.toString()))) {
            throw new SendersAccountsCurencyIsNotDinarException();
        }

        // Provera balansa sender računa
        if (sender.getBalance().compareTo(paymentDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), paymentDto.getAmount());
        }

        ClientDto clientDto = userClient.getClientById(clientId);

        // Kreiranje Payment entiteta
        Payment payment = new Payment();
        payment.setSenderName(clientDto.getFirstName() + " " + clientDto.getLastName());
        payment.setClientId(clientId);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(paymentDto.getReceiverAccountNumber());
        payment.setAmount(paymentDto.getAmount());
        payment.setPaymentCode(paymentDto.getPaymentCode());
        payment.setPurposeOfPayment(paymentDto.getPurposeOfPayment());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);

        // Postavi receiverClientId samo ako je receiver u našoj banci (za sad uvek postoji)
        payment.setReceiverClientId(receiver.getClientId());

        paymentRepository.save(payment);

        CreateVerificationRequestDto createVerificationRequestDto = new CreateVerificationRequestDto(clientId, payment.getId(), VerificationType.PAYMENT);
        userClient.createVerificationRequest(createVerificationRequestDto);

        return true;
    }

    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Account sender = payment.getSenderAccount();
        String receiverAccountNumber = payment.getAccountNumberReceiver();
        Optional<Account> receiverOpt = accountRepository.findByAccountNumber(receiverAccountNumber);

        //  Oduzimanje novca sa sender računa
        if (sender.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), payment.getAmount());
        }
        sender.setBalance(sender.getBalance().subtract(payment.getAmount()));
        accountRepository.save(sender);

        // Pronalazak bankovnog računa u osnovnoj valuti (RSD)
        Account bankAccount = accountRepository.findByAccountNumber("333000156732897612")
                .orElseThrow(() -> new BankAccountNotFoundException("Bank RSD Account not found"));

        //  Konverzija u RSD ako je potrebno
        BigDecimal rsdAmount = payment.getAmount();
        if (!sender.getCurrency().getCode().equals("RSD")) {
            rsdAmount = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), "RSD")
                    .multiply(payment.getAmount());
        }

        //  Dodavanje novca na bankovni račun
        bankAccount.setBalance(bankAccount.getBalance().add(rsdAmount));
        accountRepository.save(bankAccount);

        //  Ako je primalac u banci, konverzija i prebacivanje sredstava
        if (receiverOpt.isPresent()) {
            Account receiver = receiverOpt.get();

            //  Konverzija iz RSD u valutu primaoca
            BigDecimal convertedAmount = rsdAmount;
            if (!receiver.getCurrency().getCode().equals("RSD")) {
                convertedAmount = exchangeRateService.getExchangeRate("RSD", receiver.getCurrency().getCode())
                        .multiply(rsdAmount);
            }

            //  Dodavanje na račun primaoca
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            accountRepository.save(receiver);

            //  Oduzimanje novca sa računa banke
            bankAccount.setBalance(bankAccount.getBalance().subtract(convertedAmount));
            accountRepository.save(bankAccount);
        }

        //  Ažuriranje statusa payment-a na "COMPLETED"
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
    }

    public static BigDecimal convert(@NotNull(message = "Amount is required.") @Positive(message = "Amount must be positive.") BigDecimal amountInRSD, CurrencyType currencyType) {
        BigDecimal convertedAmount = BigDecimal.ZERO;  // Postavi početnu vrednost kao 0

        if (currencyType == CurrencyType.RSD) {
            convertedAmount = amountInRSD;
        } else if (currencyType == CurrencyType.EUR) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.0085"));
        } else if (currencyType == CurrencyType.USD) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.010"));
        } else if (currencyType == CurrencyType.HRK) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.064"));
        } else if (currencyType == CurrencyType.JPY) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("1.14"));
        } else if (currencyType == CurrencyType.GBP) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.0076"));
        } else if (currencyType == CurrencyType.AUD) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.014"));
        } else if (currencyType == CurrencyType.CHF) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.0095"));
        } else {
            throw new CurrencyNotFoundException(currencyType.toString());
        }
        return convertedAmount;
    }

    // Dohvatanje svih transakcija za određenog klijenta sa filtriranjem
    public Page<PaymentOverviewDto> getPayments(
            String token,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            PaymentStatus paymentStatus,
            String accountNumber,
            String cardNumber,
            Pageable pageable
    ) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);

        if (accountNumber != null) {
            accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(AccountNotFoundException::new);
        }

        if (cardNumber != null) {
            cardRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new CardNotFoundException(cardNumber));
        }

        Specification<Payment> spec = PaymentSpecification.filterPayments(clientId, startDate, endDate, minAmount, maxAmount, paymentStatus, accountNumber, cardNumber);
        Page<Payment> payments = paymentRepository.findAll(spec, pageable);
        return payments.map(paymentMapper::toOverviewDto);
    }

    // Dohvatanje detalja transakcije po ID-u
    public PaymentDetailsDto getPaymentDetails(String token, Long id) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);
        Payment payment = paymentRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return paymentMapper.toDetailsDto(payment);
    }


}
