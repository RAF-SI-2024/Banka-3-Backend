package rs.raf.bank_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateRepository exchangeRateRepository;
    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    private final PayeeRepository payeeRepository;

    @Override
    public void run(String... args) {
        if (currencyRepository.count() == 0) {
            // Kreiramo valute
            Currency currencyEUR = Currency.builder().code("EUR").name("Euro").symbol("€").countries("EU").description("Euro currency").active(true).build();
            Currency currencyRSD = Currency.builder().code("RSD").name("Dinar").symbol("RSD").countries("Serbia").description("Dinar currency").active(true).build();
            Currency currencyCHF = Currency.builder().code("CHF").name("Swiss Franc").symbol("CHF").countries("Switzerland").description("Swiss franc currency").active(true).build();
            Currency currencyUSD = Currency.builder().code("USD").name("US Dollar").symbol("$").countries("United States").description("US Dollar currency").active(true).build();
            Currency currencyJPY = Currency.builder().code("JPY").name("Yen").symbol("¥").countries("Japan").description("Yen currency").active(true).build();
            Currency currencyGBP = Currency.builder().code("GBP").name("British Pound").symbol("$").countries("Great Britain").description("British pound currency").active(true).build();
            Currency currencyCAD = Currency.builder().code("CAD").name("Canadian Dollar").symbol("$").countries("Canada").description("Canadian Dollar currency").active(true).build();
            Currency currencyAUD = Currency.builder().code("AUD").name("Australian Dollar").symbol("$").countries("Australia").description("Australian Dollar currency").active(true).build();

            currencyRepository.saveAll(java.util.List.of(currencyEUR, currencyRSD, currencyCHF, currencyUSD, currencyJPY, currencyGBP, currencyCAD, currencyAUD));
        }

        Currency currencyEUR = currencyRepository.findByCode("EUR").get();
        Currency currencyRSD = currencyRepository.findByCode("RSD").get();
        Currency currencyCHF = currencyRepository.findByCode("CHF").get();
        Currency currencyUSD = currencyRepository.findByCode("USD").get();
        Currency currencyJPY = currencyRepository.findByCode("JPY").get();
        Currency currencyGBP = currencyRepository.findByCode("GBP").get();
        Currency currencyCAD = currencyRepository.findByCode("CAD").get();
        Currency currencyAUD = currencyRepository.findByCode("AUD").get();

        if (accountRepository.count() == 0) {
            // Get existing account numbers to avoid conflicts
            List<String> existingAccountNumbers = accountRepository.findAll()
                    .stream()
                    .map(Account::getAccountNumber)
                    .collect(Collectors.toList());

            List<Card> newCards = new ArrayList<>();
            List<Payment> newPayments = new ArrayList<>();

            // Generate accounts for all 10 clients with diverse profiles
            for (int i = 1; i <= 10; i++) {
                // Generate unique account number
                String accountNumber;
                do {
                    String prefix = i % 3 == 0 ? "840" : (i % 3 == 1 ? "978" : "840"); // RSD, EUR, or RSD
                    accountNumber = prefix + String.format("%013d", 1000000L + i);
                } while (existingAccountNumbers.contains(accountNumber));

                // Create account with diverse characteristics
                Account account = PersonalAccount.builder()
                        .name("Client " + i + " Account")
                        .accountNumber(accountNumber)
                        .clientId((long) i)
                        .createdByEmployeeId(3L)
                        .creationDate(LocalDate.now().minusMonths(i % 12))
                        .expirationDate(LocalDate.now().plusYears(2))
                        .currency(i % 3 == 0 ? currencyRSD : (i % 3 == 1 ? currencyEUR : currencyRSD))
                        .status(AccountStatus.ACTIVE)
                        .balance(BigDecimal.valueOf(1000 + (i * 2000))) // Varying balances
                        .availableBalance(BigDecimal.valueOf(1000 + (i * 2000)))
                        .dailyLimit(BigDecimal.valueOf(1000 + (i * 500)))
                        .monthlyLimit(BigDecimal.valueOf(5000 + (i * 2000)))
                        .dailySpending(BigDecimal.ZERO)
                        .monthlySpending(BigDecimal.ZERO)
                        .type(i % 2 == 0 ? AccountType.CURRENT : AccountType.FOREIGN)
                        .accountOwnerType(AccountOwnerType.PERSONAL)
                        .build();

                // Save account
                account = accountRepository.save(account);
                existingAccountNumbers.add(accountNumber);

                // Create card based on client profile
                if (i != 3 && i != 7) { // Clients 3 and 7 don't get cards (poor credit)
                    String cardNumber;
                    do {
                        cardNumber = "4" + String.format("%015d", 1000000L + i);
                    } while (cardRepository.findByCardNumber(cardNumber).isPresent());

                    CardType cardType = i % 2 == 0 ? CardType.CREDIT : CardType.DEBIT;
                    BigDecimal cardLimit = cardType == CardType.CREDIT ? 
                        account.getDailyLimit().multiply(BigDecimal.valueOf(3)) : 
                        account.getDailyLimit();

                    Card card = Card.builder()
                            .cardNumber(cardNumber)
                            .account(account)
                            .name("Card for " + account.getName())
                            .cvv("123")
                            .creationDate(LocalDate.now().minusMonths(1))
                            .expirationDate(LocalDate.now().plusYears(3))
                            .type(cardType)
                            .status(CardStatus.ACTIVE)
                            .issuer(i % 2 == 0 ? CardIssuer.VISA : CardIssuer.MASTERCARD)
                            .cardLimit(cardLimit)
                            .build();

                    card = cardRepository.save(card);
                    newCards.add(card);
                }
            }

            // Create diverse transactions for all accounts
            List<Account> allAccounts = accountRepository.findAll();
            for (Account senderAccount : allAccounts) {
                // Generate varying number of transactions based on client profile
                int transactionCount = (int) (Math.random() * 30) + 5;
                
                for (int i = 0; i < transactionCount; i++) {
                    // Find a different receiver account
                    Account receiverAccount;
                    do {
                        receiverAccount = allAccounts.get((int) (Math.random() * allAccounts.size()));
                    } while (receiverAccount.getAccountNumber().equals(senderAccount.getAccountNumber()));

                    // Create payment with varying amounts
                    BigDecimal amount = BigDecimal.valueOf((Math.random() * 3000) + 50)
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    Payment payment = Payment.builder()
                            .senderName("Sender " + senderAccount.getClientId())
                            .clientId(senderAccount.getClientId())
                            .senderAccount(senderAccount)
                            .amount(amount)
                            .date(LocalDateTime.now().minusDays(i))
                            .status(Math.random() > 0.1 ? PaymentStatus.COMPLETED : PaymentStatus.CANCELED)
                            .purposeOfPayment("Transaction " + i)
                            .referenceNumber(String.format("REF%08d", i))
                            .accountNumberReceiver(receiverAccount.getAccountNumber())
                            .receiverClientId(receiverAccount.getClientId())
                            .build();

                    // Assign card based on probability
                    if (Math.random() > 0.6 && !senderAccount.getCards().isEmpty()) {
                        payment.setCard(senderAccount.getCards().get(0));
                    }

                    // Handle foreign currency transactions
                    if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
                        payment.setOutAmount(amount.multiply(BigDecimal.valueOf(0.95)));
                        payment.setExchangeProfit(amount.multiply(BigDecimal.valueOf(0.05)));
                    }

                    payment = paymentRepository.save(payment);
                    newPayments.add(payment);
                }
            }

            // Create diverse loans and installments
            List<Loan> newLoans = new ArrayList<>();
            List<Installment> newInstallments = new ArrayList<>();

            // Create different loan scenarios for eligible accounts
            for (Account account : allAccounts) {
                // Skip loans for clients 3 and 7 (poor credit)
                if (account.getClientId() == 3L || account.getClientId() == 7L) {
                    continue;
                }

                // Random number of loans per account (0-2)
                int loanCount = (int) (Math.random() * 3);
                
                for (int j = 0; j < loanCount; j++) {
                    // Generate loan amount based on account balance and client profile
                    BigDecimal maxLoanAmount = account.getBalance().multiply(BigDecimal.valueOf(1.5));
                    BigDecimal loanAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.3 + (Math.random() * 0.7)))
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    // Random loan term in months (12-60)
                    int termMonths = (int) (Math.random() * 48) + 12;
                    
                    // Calculate monthly installment with varying interest rates
                    BigDecimal monthlyRate = BigDecimal.valueOf(0.008 + (Math.random() * 0.004)); // 0.8% - 1.2% monthly
                    BigDecimal monthlyPayment = calculateMonthlyPayment(loanAmount, monthlyRate, termMonths);
                    
                    // Generate loan number
                    String loanNumber = "LN" + String.format("%08d", 10000000L + (j * 100));

                    // Create loan with varying statuses
                    LoanStatus status = Math.random() > 0.1 ? LoanStatus.APPROVED : LoanStatus.REJECTED;
                    if (status == LoanStatus.APPROVED) {
                        Loan loan = Loan.builder()
                                .loanNumber(loanNumber)
                                .type(LoanType.CASH)
                                .amount(loanAmount)
                                .repaymentPeriod(termMonths)
                                .nominalInterestRate(monthlyRate.multiply(BigDecimal.valueOf(12)))
                                .effectiveInterestRate(monthlyRate.multiply(BigDecimal.valueOf(12.68)))
                                .startDate(LocalDate.now().minusMonths((int) (Math.random() * termMonths)))
                                .dueDate(LocalDate.now().plusMonths(termMonths))
                                .nextInstallmentAmount(monthlyPayment)
                                .nextInstallmentDate(LocalDate.now().plusMonths(1))
                                .remainingDebt(loanAmount)
                                .currency(account.getCurrency())
                                .status(status)
                                .interestRateType(InterestRateType.FIXED)
                                .account(account)
                                .build();

                        loan = loanRepository.save(loan);
                        newLoans.add(loan);

                        // Create installments with varying payment statuses
                        LocalDate startDate = loan.getStartDate();
                        for (int i = 0; i < termMonths; i++) {
                            LocalDate dueDate = startDate.plusMonths(i + 1);
                            boolean isPaid = dueDate.isBefore(LocalDate.now()) && Math.random() > 0.2; // 80% chance of payment
                            
                            Installment installment = Installment.builder()
                                    .loan(loan)
                                    .amount(monthlyPayment)
                                    .actualDueDate(dueDate)
                                    .installmentStatus(isPaid ? InstallmentStatus.PAID : InstallmentStatus.UNPAID)
                                    .expectedDueDate(isPaid ? dueDate : null)
                                    .build();

                            installment = installmentRepository.save(installment);
                            newInstallments.add(installment);
                        }
                    }
                }
            }

            // Save all installments
            installmentRepository.saveAll(newInstallments);
        }

        // Kreiranje kursne liste
//        exchangeRateService.updateExchangeRates();

        if (exchangeRateRepository.count() == 0) {
            // Test kursna lista da ne trosimo API pozive
            ExchangeRate rsdToEur = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyEUR)
                    .exchangeRate(new BigDecimal("0.008540"))
                    .sellRate(new BigDecimal("0.008540").multiply(new BigDecimal("0.995")))
                    .build();

            ExchangeRate rsdToUsd = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyUSD)
                    .exchangeRate(new BigDecimal("0.009257"))
                    .sellRate(new BigDecimal("0.009257").multiply(new BigDecimal("0.995")))
                    .build();

            ExchangeRate rsdToChf = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyCHF)
                    .exchangeRate(new BigDecimal("0.008129"))
                    .sellRate(new BigDecimal("0.008129").multiply(new BigDecimal("0.995")))
                    .build();

            ExchangeRate rsdToJpy = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyJPY)
                    .exchangeRate(new BigDecimal("1.363100"))
                    .sellRate(new BigDecimal("1.363100").multiply(new BigDecimal("0.995")))
                    .build();

            ExchangeRate rsdToCad = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyCAD)
                    .exchangeRate(new BigDecimal("0.013350"))
                    .sellRate(new BigDecimal("0.013350").multiply(new BigDecimal("0.995")))
                    .build();

            ExchangeRate rsdToAud = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyAUD)
                    .exchangeRate(new BigDecimal("0.014670"))
                    .sellRate(new BigDecimal("0.014670").multiply(new BigDecimal("0.995")))
                    .build();

            ExchangeRate rsdToGbp = ExchangeRate.builder()
                    .fromCurrency(currencyRSD)
                    .toCurrency(currencyGBP)
                    .exchangeRate(new BigDecimal("0.007172"))
                    .sellRate(new BigDecimal("0.007172").multiply(new BigDecimal("0.995")))
                    .build();

            List<ExchangeRate> exchangeRates = java.util.List.of(
                    rsdToEur, rsdToUsd, rsdToChf, rsdToGbp, rsdToJpy, rsdToAud, rsdToCad
            );

            List<ExchangeRate> exchangeRates2 = new ArrayList<>();

            for (ExchangeRate exchangeRate : exchangeRates) {
                exchangeRates2.add(ExchangeRate.builder()
                        .fromCurrency(exchangeRate.getToCurrency())
                        .toCurrency(exchangeRate.getFromCurrency())
                        .exchangeRate(BigDecimal.ONE.divide(exchangeRate.getExchangeRate(), 6, RoundingMode.UP))
                        .sellRate(BigDecimal.ONE.divide(exchangeRate.getExchangeRate(), 6, RoundingMode.UP).multiply(new BigDecimal("0.995")))
                        .build());
            }

            exchangeRateRepository.saveAll(exchangeRates);
            exchangeRateRepository.saveAll(exchangeRates2);
            // Test kursna lista da ne trosimo API pozive
        }

    }

    private BigDecimal calculateMonthlyPayment(BigDecimal loanAmount, BigDecimal monthlyRate, int termMonths) {
        // PMT = P * (r * (1 + r)^n) / ((1 + r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(termMonths);
        BigDecimal numerator = monthlyRate.multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        
        return loanAmount.multiply(numerator.divide(denominator, 2, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
