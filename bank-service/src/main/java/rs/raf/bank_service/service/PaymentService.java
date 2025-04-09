package rs.raf.bank_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.Banka2UserClient;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.domain.mapper.PaymentMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.specification.PaymentSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final Banka2UserClient banka2UserClient;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;
    private final ExchangeRateService exchangeRateService;
    private final TransactionQueueService transactionQueueService;
    private PaymentRepository paymentRepository;
    private CardRepository cardRepository;
    private CompanyAccountRepository companyAccountRepository;
    private AccountService accountService;
    private CurrencyRepository currencyRepository;

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
        Account receiver = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver())
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
        Account sender = accountRepository.findByAccountNumberAndClientId(paymentDto.getSenderAccountNumber(), clientId)
                .stream().findFirst()
                .orElseThrow(() -> new SenderAccountNotFoundException(paymentDto.getSenderAccountNumber()));

        if (!accountService.getBankCode(paymentDto.getReceiverAccountNumber()).equals("222")) {

            Account receiver = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber())
                    .stream().findFirst()
                    .orElseThrow(() -> new ReceiverAccountNotFoundException(paymentDto.getReceiverAccountNumber()));


            // Provera balansa sender računa
            if (sender.getBalance().compareTo(paymentDto.getAmount()) < 0) {
                throw new InsufficientFundsException(sender.getBalance(), paymentDto.getAmount());
            }

            ClientDto clientDto = userClient.getClientById(clientId);

            BigDecimal amount = paymentDto.getAmount();
            BigDecimal convertedAmount = amount;
            BigDecimal exchangeRateValue = BigDecimal.ONE;

            // Provera da li su valute različite
            if (!sender.getCurrency().equals(receiver.getCurrency())) {
                ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
                exchangeRateValue = exchangeRateDto.getSellRate();
                convertedAmount = amount.multiply(exchangeRateValue);
            }

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
            payment.setOutAmount(convertedAmount);

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
        } else {
            Optional<Account> reciever = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber());
            if (reciever.isEmpty()){
                Optional<Banka2AccountResponseDto> banka2AccountResponseDtoOptional = banka2UserClient.getAccountByAccountNumber(paymentDto.getReceiverAccountNumber());

                if (banka2AccountResponseDtoOptional.isEmpty()){
                    throw new ExternalAccountNotFoundException(paymentDto.getReceiverAccountNumber());
                }
                Banka2AccountResponseDto banka2AccountResponseDto = banka2AccountResponseDtoOptional.get();


                Account account = new PersonalAccount();
                account.setAccountNumber(banka2AccountResponseDto.getItems().get(0).getAccountNumber());
                account.setName(banka2AccountResponseDto.getItems().get(0).getName());
                account.setClientId(null);
                account.setCreatedByEmployeeId(null);
                String creationDateStr = banka2AccountResponseDto.getItems().get(0).getCreationDate();
                LocalDate creationDate = LocalDate.parse(creationDateStr);
                account.setCreationDate(creationDate);
                String expirationDateStr =  banka2AccountResponseDto.getItems().get(0).getExpirationDate();
                LocalDate expirationDate = LocalDate.parse(expirationDateStr);
                account.setExpirationDate(expirationDate);
                account.setCurrency(currencyRepository.findByCode(banka2AccountResponseDto.getItems().get(0).getCurrency().getCode()).get());
                account.setStatus(AccountStatus.ACTIVE);
                account.setType(AccountType.FOREIGN);
                account.setAccountOwnerType(AccountOwnerType.PERSONAL);
                account.setBalance(BigDecimal.valueOf(banka2AccountResponseDto.getItems().get(0).getBalance()));
                account.setAvailableBalance(BigDecimal.valueOf(banka2AccountResponseDto.getItems().get(0).getAvailableBalance()));
                account.setDailyLimit(BigDecimal.valueOf(banka2AccountResponseDto.getItems().get(0).getDailyLimit()));
                account.setMonthlyLimit(BigDecimal.valueOf(banka2AccountResponseDto.getItems().get(0).getMonthlyLimit()));
                account.setDailySpending(BigDecimal.ZERO);
                account.setMonthlySpending(BigDecimal.ZERO);
                // samo ne znam da li treba personal i ako ne kako ga iz njihovog json-a izvuci

                accountRepository.save(account);
            }
            return new PaymentDto();
        }
    }

    public void handleTax(TaxDto taxDto) throws JsonProcessingException {

        CreatePaymentDto createPaymentDto = new CreatePaymentDto();
        createPaymentDto.setPurposeOfPayment("tax");
        createPaymentDto.setSenderAccountNumber(taxDto.getSenderAccountNumber());
        createPaymentDto.setAmount(taxDto.getAmount());
        createPaymentDto.setReferenceNumber("N/A");
        createPaymentDto.setPaymentCode("N/A");
        createPaymentDto.setRecieverName("Republika Srbija");
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
}
