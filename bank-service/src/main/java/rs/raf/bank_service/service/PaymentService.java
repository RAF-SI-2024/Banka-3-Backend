package rs.raf.bank_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.Banka2Client;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.configuration.RabbitMQConfig;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.domain.enums.TransactionType;
import rs.raf.bank_service.domain.enums.VerificationType;
import rs.raf.bank_service.domain.mapper.PaymentMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.repository.CompanyAccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.specification.PaymentSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;
    private final ExchangeRateService exchangeRateService;
    private final TransactionQueueService transactionQueueService;
    private PaymentRepository paymentRepository;
    private CardRepository cardRepository;
    private CompanyAccountRepository companyAccountRepository;
    private static final String OWN_BANK_CODE = "333";
    private final Banka2Client partnerBankClient;
    private final RabbitTemplate rabbitTemplate;

    public boolean createTransferPendingConfirmation(TransferDto transferDto, Long clientId) throws JsonProcessingException {
        // Preuzimanje računa za sender i receiver
        Account sender = accountRepository.findByAccountNumberAndClientId(transferDto.getSenderAccountNumber(), clientId)
                .stream().findFirst()
                .orElseThrow(() -> new SenderAccountNotFoundException(transferDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(transferDto.getReceiverAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(transferDto.getReceiverAccountNumber()));

        // Provera da li sender ima dovoljno sredstava
        if (sender.getBalance().compareTo(transferDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), transferDto.getAmount());
        }

        BigDecimal amount = transferDto.getAmount();
        BigDecimal convertedAmount = amount;
        BigDecimal exchangeRateValue = BigDecimal.ONE;

        // Provera da li su valute različite
        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            // Dobijanje kursa konverzije
            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getSellRate();

            // Konverzija iznosa u valutu receiver-a
            convertedAmount = amount.multiply(exchangeRateValue);
        }

        // Kreiranje Payment entiteta za transfer
        Payment payment = new Payment();
        payment.setClientId(clientId);  // Dodajemo Client ID
        payment.setSenderAccount(sender);  // Sender račun
        payment.setAmount(transferDto.getAmount());  // Iznos
        payment.setAccountNumberReceiver(transferDto.getReceiverAccountNumber());  // Primalac (receiver)
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);  // Status je "na čekanju"
        payment.setDate(LocalDateTime.now());  // Datum transakcije
        payment.setOutAmount(convertedAmount);

        // Postavi receiverClientId samo ako je receiver u našoj banci
        payment.setReceiverClientId(receiver.getClientId());  // Postavljamo receiverClientId

        paymentRepository.save(payment);

        PaymentVerificationDetailsDto paymentVerificationDetailsDto = PaymentVerificationDetailsDto.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(transferDto.getReceiverAccountNumber())
                .amount(transferDto.getAmount())
                .build();

        // Kreiraj PaymentVerificationRequestDto i pozovi UserClient da kreira verificationRequest
        CreateVerificationRequestDto paymentVerificationRequestDto = new CreateVerificationRequestDto(
                clientId,
                payment.getId(),
                VerificationType.TRANSFER,
                objectMapper.writeValueAsString(paymentVerificationDetailsDto)
        );
        userClient.createVerificationRequest(paymentVerificationRequestDto);
        return true;
    }

    @Transactional
    public boolean confirmTransferAndExecute(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Account sender = payment.getSenderAccount();
        String receiverAccountNumber = payment.getAccountNumberReceiver();
        String receiverBankCode = receiverAccountNumber.length() >= 3 ? receiverAccountNumber.substring(0, 3) : "";
        BigDecimal amount = payment.getAmount();

        if (!receiverBankCode.equals(OWN_BANK_CODE)) {
            if (sender.getAvailableBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(sender.getAvailableBalance(), amount);
            }

            sender.setAvailableBalance(sender.getAvailableBalance().subtract(amount));
            accountRepository.save(sender);

            InterBankTransactionRequest request = new InterBankTransactionRequest();
            request.setFromAccountNumber(sender.getAccountNumber());
            request.setFromCurrencyId(sender.getCurrency().getCode());
            request.setToAccountNumber(receiverAccountNumber);
            request.setToCurrencyId(sender.getCurrency().getCode());
            request.setAmount(amount);
            request.setCodeId(payment.getPaymentCode());
            request.setPurpose(payment.getPurposeOfPayment());
            request.setReferenceNumber(payment.getReferenceNumber());

            InterBankTransactionResponse response;
            try {
                response = partnerBankClient.prepare(request).getBody();
            } catch (Exception e) {
                sender.setAvailableBalance(sender.getAvailableBalance().add(amount));
                accountRepository.save(sender);
                throw new ReceiverAccountNotFoundException("Greska u komunikaciji sa bankom 2 (prepare): " + e.getMessage());
            }

            if (response == null || !response.isReady()) {
                sender.setAvailableBalance(sender.getAvailableBalance().add(amount));
                accountRepository.save(sender);
                throw new RuntimeException("Banka 2 nije spremna: " + (response != null ? response.getMessage() : "Nepoznata greska"));
            }

            try {
                partnerBankClient.commit(request);
            } catch (Exception e) {
                try {
                    partnerBankClient.cancel(request);
                } catch (Exception cancelError) {
                    System.err.println("Neuspesan cancel kod banke 2: " + cancelError.getMessage());
                }

                sender.setAvailableBalance(sender.getAvailableBalance().add(amount));
                accountRepository.save(sender);

                throw new RuntimeException("Commit kod banke 2 nije uspeo: " + e.getMessage());
            }

            sender.setBalance(sender.getBalance().subtract(amount));
            sender.setAvailableBalance(sender.getBalance());
            accountRepository.save(sender);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setOutAmount(response.getFinalAmount());
            paymentRepository.save(payment);

            return true;
        }

        Account receiver = accountRepository.findByAccountNumber(receiverAccountNumber)
                .orElseThrow(() -> new ReceiverAccountNotFoundException(receiverAccountNumber));

        BigDecimal convertedAmount = amount;
        BigDecimal exchangeRateValue = BigDecimal.ONE;

        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            CompanyAccount bankAccountFrom = accountRepository.findFirstByCurrencyAndCompanyId(sender.getCurrency(), 1L)
                    .orElseThrow(() -> new BankAccountNotFoundException("Nema bankovnog računa za: " + sender.getCurrency().getCode()));

            CompanyAccount bankAccountTo = accountRepository.findFirstByCurrencyAndCompanyId(receiver.getCurrency(), 1L)
                    .orElseThrow(() -> new BankAccountNotFoundException("Nema bankovnog računa za: " + receiver.getCurrency().getCode()));

            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getExchangeRate();
            convertedAmount = amount.multiply(exchangeRateValue);

            sender.setBalance(sender.getBalance().subtract(amount));
            sender.setAvailableBalance(sender.getBalance());
            bankAccountFrom.setBalance(bankAccountFrom.getBalance().add(amount));
            bankAccountFrom.setAvailableBalance(bankAccountFrom.getBalance());
            accountRepository.save(sender);
            accountRepository.save(bankAccountFrom);

            bankAccountTo.setBalance(bankAccountTo.getBalance().subtract(convertedAmount));
            bankAccountTo.setAvailableBalance(bankAccountTo.getBalance());
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            receiver.setAvailableBalance(receiver.getBalance());
            accountRepository.save(bankAccountTo);
        } else {
            sender.setBalance(sender.getBalance().subtract(amount));
            sender.setAvailableBalance(sender.getBalance());
            receiver.setBalance(receiver.getBalance().add(amount));
            receiver.setAvailableBalance(receiver.getBalance());
        }

        accountRepository.save(sender);
        accountRepository.save(receiver);

        payment.setOutAmount(convertedAmount);
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        return true;
    }


    public void rejectTransfer(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getStatus().equals(PaymentStatus.PENDING_CONFIRMATION))
            throw new RejectNonPendingRequestException();

        payment.setStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);
    }


    public PaymentDto createPaymentBeforeConfirmation(CreatePaymentDto paymentDto, Long clientId) throws JsonProcessingException {
        if (paymentDto.getPaymentCode() == null || paymentDto.getPaymentCode().isEmpty()) {
            throw new PaymentCodeNotProvidedException();
        }

        if (paymentDto.getPurposeOfPayment() == null || paymentDto.getPurposeOfPayment().isEmpty()) {
            throw new PurposeOfPaymentNotProvidedException();
        }

        // Preuzimanje sender računa
        Account sender = accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new AccNotFoundException(paymentDto.getSenderAccountNumber()));
        //ovo ce da se menja 90%
        String senderNameSurname;
        if (sender instanceof CompanyAccount && ((CompanyAccount) sender).getCompanyId() == 1) {
            senderNameSurname = "Banka 2";
        }else {
            ClientDto clientDto = userClient.getClientById(clientId);
            senderNameSurname = clientDto.getFirstName() + " " + clientDto.getLastName();
        }

        Account receiver = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber())
                .stream().findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(paymentDto.getReceiverAccountNumber()));


        // Provera balansa sender računa
        if (sender.getBalance().compareTo(paymentDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), paymentDto.getAmount());
        }


        BigDecimal amount = paymentDto.getAmount();
        BigDecimal convertedAmount = amount;
        BigDecimal exchangeRateValue = BigDecimal.ONE;
        BigDecimal exchangeProfit = BigDecimal.ZERO;
        // Provera da li su valute različite
        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getSellRate();
            convertedAmount = amount.multiply(exchangeRateValue);

            exchangeProfit = exchangeRateDto.getSellRate().multiply(amount)
                    .subtract(exchangeRateDto.getExchangeRate().multiply(amount));
        }

        // Kreiranje Payment entiteta
        Payment payment = new Payment();
        payment.setSenderName(senderNameSurname);
        payment.setClientId(clientId);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(paymentDto.getReceiverAccountNumber());
        payment.setAmount(paymentDto.getAmount());
        payment.setPaymentCode(paymentDto.getPaymentCode());
        payment.setPurposeOfPayment(paymentDto.getPurposeOfPayment());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setOutAmount(convertedAmount);
        payment.setExchangeProfit(exchangeProfit);

        // Postavi receiverClientId samo ako je receiver u našoj banci (za sad uvek postoji)
        payment.setReceiverClientId(receiver.getClientId());

        paymentRepository.save(payment);

        PaymentVerificationDetailsDto paymentVerificationDetailsDto = PaymentVerificationDetailsDto.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(paymentDto.getReceiverAccountNumber())
                .amount(paymentDto.getAmount())
                .build();

        CreateVerificationRequestDto createVerificationRequestDto = new CreateVerificationRequestDto(clientId, payment.getId(), VerificationType.PAYMENT, objectMapper.writeValueAsString(paymentVerificationDetailsDto));
        userClient.createVerificationRequest(createVerificationRequestDto);

        return paymentMapper.toPaymentDto(payment, paymentDto.getRecieverName());
    }

    public void handleTax(TaxDto taxDto) throws JsonProcessingException {
        CreatePaymentDto createPaymentDto = new CreatePaymentDto();
        createPaymentDto.setPurposeOfPayment("tax");
        createPaymentDto.setSenderAccountNumber(taxDto.getSenderAccountNumber());
        createPaymentDto.setReferenceNumber("N/A");
        createPaymentDto.setPaymentCode("N/A");
        createPaymentDto.setRecieverName("Republika Srbija");
        createPaymentDto.setAmount(taxDto.getAmount());
        Account account = companyAccountRepository.findByCompanyId(2L);
        createPaymentDto.setReceiverAccountNumber(account.getAccountNumber());
        PaymentDto paymentDto = createPaymentBeforeConfirmation(createPaymentDto, taxDto.getClientId());
        transactionQueueService.queueTransaction(TransactionType.CONFIRM_PAYMENT, paymentDto.getId());
    }

    @Transactional
    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Account sender = payment.getSenderAccount();

        String receiverBankCode = payment.getAccountNumberReceiver().substring(0, 3);

        // Ako je interbank → pošalji u delay queue, ne izvršavaj odmah
        if (!receiverBankCode.equals(OWN_BANK_CODE)) {
            rabbitTemplate.convertAndSend(RabbitMQConfig.INTERBANK_DELAY_QUEUE, paymentId);
            return;
        }

        Account receiver = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver())
                .stream().findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(payment.getAccountNumberReceiver()));

        BigDecimal amount = payment.getAmount();
        BigDecimal convertedAmount = amount;
        BigDecimal exchangeRateValue = BigDecimal.ONE;

        //  Ako su valute različite, koristimo kursnu listu
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            //  Obezbeđujemo da transakcije idu preko bankovnih računa (companyId = 1)
            CompanyAccount bankAccountFrom = accountRepository.findFirstByCurrencyAndCompanyId(sender.getCurrency(), 1L)
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + sender.getCurrency().getCode()));

            CompanyAccount bankAccountTo = accountRepository.findFirstByCurrencyAndCompanyId(receiver.getCurrency(), 1L)
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + receiver.getCurrency().getCode()));

            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getExchangeRate();
            convertedAmount = amount.multiply(exchangeRateValue);

            //  Sender -> Banka (ista valuta)
            sender.setBalance(sender.getBalance().subtract(amount));
            sender.setAvailableBalance(sender.getBalance());
            bankAccountFrom.setBalance(bankAccountFrom.getBalance().add(amount));
            bankAccountFrom.setAvailableBalance(bankAccountFrom.getBalance());
            accountRepository.save(sender);
            accountRepository.save(bankAccountFrom);

            //  Banka -> Receiver
            bankAccountTo.setBalance(bankAccountTo.getBalance().subtract(convertedAmount));
            bankAccountTo.setAvailableBalance(bankAccountTo.getBalance());
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            receiver.setAvailableBalance(receiver.getBalance());
            accountRepository.save(bankAccountTo);
        } else {
            sender.setBalance(sender.getBalance().subtract(amount));
            sender.setAvailableBalance(sender.getBalance());
            receiver.setBalance(receiver.getBalance().add(amount));
            receiver.setAvailableBalance(receiver.getBalance());
        }

        accountRepository.save(sender);
        accountRepository.save(receiver);

        //  Čuvamo outAmount u Payment (stvarno primljen iznos)
        payment.setOutAmount(convertedAmount);
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
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

    public void rejectPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getStatus().equals(PaymentStatus.PENDING_CONFIRMATION))
            throw new RejectNonPendingRequestException();

        payment.setStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);
    }

    public BigDecimal getExchangeProfit() {
        return paymentRepository.getBankProfitFromExchange();
    }

}
