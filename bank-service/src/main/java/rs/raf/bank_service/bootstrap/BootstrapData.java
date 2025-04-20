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

            // Client profiles (1-5 from user service)
            // Client 1: High-value client
            // Client 2: Average client with stable history
            // Client 3: Low-activity client
            // Client 4: Client with mixed history
            // Client 5: New client with minimal history

            for (int i = 1; i <= 10; i++) {
                // Generate unique account number
                String accountNumber;
                int attempts = 0;
                do {
                    int currencyType = (int)(i % 3);
                    String prefix = switch (currencyType) {
                        case 0 -> "840"; // USD
                        case 1 -> "978"; // EUR
                        case 2 -> "941"; // RSD
                        default -> "840";
                    };
                    accountNumber = prefix + String.format("%013d", 1000000L + i + (attempts * 1000));
                    attempts++;
                    if (attempts > 100) {
                        throw new RuntimeException("Failed to generate unique account number after 100 attempts");
                    }
                } while (existingAccountNumbers.contains(accountNumber));

                // Set balance and limits based on client profile
                BigDecimal balance;
                BigDecimal dailyLimit;
                BigDecimal monthlyLimit;
                AccountType accountType;
                
                // Determine currency based on client number
                int currencyType = (int)(i % 3);
                Currency accountCurrency = switch (currencyType) {
                    case 0 -> currencyUSD;
                    case 1 -> currencyEUR;
                    case 2 -> currencyRSD;
                    default -> currencyRSD;
                };

                // Determine client type based on binomial distribution
                int clientType = (int)(i % 10); // Using modulo 10 for more granular control
                
                // Base values that will be modified by specific client number
                BigDecimal baseBalance = BigDecimal.ZERO;
                BigDecimal baseLimit = BigDecimal.ZERO;
                accountType = AccountType.CURRENT;
                
                // Set base characteristics by client type pattern
                switch (clientType) {
                    case 0, 1 -> { // High-risk clients (2 clients)
                        baseBalance = BigDecimal.valueOf(1000);
                        baseLimit = BigDecimal.valueOf(500);
                        // Characteristics: Low balance, declining activity, high failure rate
                    }
                    case 2 -> { // Moderately low-risk client (1 client)
                        baseBalance = BigDecimal.valueOf(5000);
                        baseLimit = BigDecimal.valueOf(1000);
                        // Characteristics: Moderate balance, stable but low activity
                    }
                    case 3, 4, 5, 6 -> { // Middle-risk clients (4 clients)
                        baseBalance = BigDecimal.valueOf(15000);
                        baseLimit = BigDecimal.valueOf(2000);
                        // Characteristics: Good balance, regular activity, occasional failures
                    }
                    case 7 -> { // Moderately high-risk client (1 client)
                        baseBalance = BigDecimal.valueOf(30000);
                        baseLimit = BigDecimal.valueOf(3000);
                        accountType = AccountType.FOREIGN;
                        // Characteristics: High balance, good activity, rare failures
                    }
                    case 8, 9 -> { // High-value clients (2 clients)
                        baseBalance = BigDecimal.valueOf(50000);
                        baseLimit = BigDecimal.valueOf(5000);
                        accountType = AccountType.FOREIGN;
                        // Characteristics: Very high balance, excellent activity, minimal failures
                    }
                }

                // Modify base values based on specific client number to create variety
                double balanceMultiplier = 1.0 + (i * 0.1); // Each client gets progressively larger modification
                double randomFactor = Math.random() * 0.5 + 0.75; // Random factor between 0.75 and 1.25

                balance = baseBalance.multiply(BigDecimal.valueOf(balanceMultiplier * randomFactor));
                dailyLimit = baseLimit.multiply(BigDecimal.valueOf(randomFactor));
                monthlyLimit = dailyLimit.multiply(BigDecimal.valueOf(10)); // Monthly limit is 10x daily

                // Create account with diverse characteristics
                Account account = PersonalAccount.builder()
                        .name("Client " + i + " Account")
                        .accountNumber(accountNumber)
                        .clientId((long) i)
                        .createdByEmployeeId(3L)
                        .creationDate(LocalDate.now().minusMonths(i * 3)) // Different ages spread across 30 months
                        .expirationDate(LocalDate.now().plusYears(2))
                        .currency(accountCurrency)
                        .status(AccountStatus.ACTIVE)
                        .balance(balance)
                        .availableBalance(balance)
                        .dailyLimit(dailyLimit)
                        .monthlyLimit(monthlyLimit)
                        .dailySpending(BigDecimal.ZERO)
                        .monthlySpending(BigDecimal.ZERO)
                        .type(accountType)
                        .accountOwnerType(AccountOwnerType.PERSONAL)
                        .build();

                account = accountRepository.save(account);
                existingAccountNumbers.add(accountNumber);

                // Create cards based on client profile and creditworthiness
                if (clientType < 2) { // High-risk clients: no cards
                    // Skip card creation
                } else if (clientType == 2) { // Moderately low-risk: debit card only
                    createDebitCard(account, dailyLimit, newCards);
                } else if (clientType >= 3 && clientType <= 6) { // Middle-risk: debit card with chance of credit card
                    createDebitCard(account, dailyLimit, newCards);
                    if (Math.random() > 0.5) {
                        createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(2)), newCards);
                    }
                } else if (clientType == 7) { // Moderately high-risk: credit card
                    createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(3)), newCards);
                } else { // High-value: multiple credit cards
                    createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(5)), newCards);
                    createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(4)), newCards);
                }

                // Create second account for high-value clients
                if (clientType >= 8) {
                    String secondAccountNumber;
                    do {
                        secondAccountNumber = "978" + String.format("%013d", 2000000L + i);
                    } while (existingAccountNumbers.contains(secondAccountNumber));

                    Account secondAccount = PersonalAccount.builder()
                            .name("Client " + i + " Savings Account")
                            .accountNumber(secondAccountNumber)
                            .clientId((long) i)
                            .createdByEmployeeId(3L)
                            .creationDate(LocalDate.now().minusMonths(3))
                            .expirationDate(LocalDate.now().plusYears(2))
                            .currency(currencyEUR)
                            .status(AccountStatus.ACTIVE)
                            .balance(balance.multiply(BigDecimal.valueOf(0.4))) // 40% of main account
                            .availableBalance(balance.multiply(BigDecimal.valueOf(0.4)))
                            .dailyLimit(dailyLimit.multiply(BigDecimal.valueOf(0.6))) // 60% of main account
                            .monthlyLimit(monthlyLimit.multiply(BigDecimal.valueOf(0.6)))
                            .dailySpending(BigDecimal.ZERO)
                            .monthlySpending(BigDecimal.ZERO)
                            .type(AccountType.FOREIGN)
                            .accountOwnerType(AccountOwnerType.PERSONAL)
                            .build();

                    secondAccount = accountRepository.save(secondAccount);
                    existingAccountNumbers.add(secondAccountNumber);
                }
            }

            // Create diverse transactions with meaningful patterns
            List<Account> allAccounts = accountRepository.findAll();
            for (Account senderAccount : allAccounts) {
                // Calculate base transaction count using client type
                int clientType = (int)(senderAccount.getClientId() % 10);
                int baseTransactions = 5;
                
                // Determine additional transactions based on client type pattern
                int additionalTransactions = switch (clientType) {
                    case 0, 1 -> 5;  // High-risk: minimal transactions
                    case 2 -> 10;    // Moderately low-risk: few transactions
                    case 3, 4, 5, 6 -> 15; // Middle-risk: regular transactions
                    case 7 -> 20;    // Moderately high-risk: frequent transactions
                    default -> 30;   // High-value: many transactions
                };

                // Modify transaction count based on specific client number
                additionalTransactions = (int)(additionalTransactions * (1.0 + (senderAccount.getClientId() % 3) * 0.1));
                
                int transactionCount = baseTransactions + additionalTransactions;
                
                for (int i = 0; i < transactionCount; i++) {
                    Account receiverAccount;
                    do {
                        receiverAccount = allAccounts.get((int) (Math.random() * allAccounts.size()));
                    } while (receiverAccount.getAccountNumber().equals(senderAccount.getAccountNumber()));

                    // Calculate transaction date with churn risk patterns
                    long daysAgo;
                    if (clientType >= 8) { // High-value: consistent recent activity
                        daysAgo = (long)(Math.random() * 30);
                    } else if (clientType == 7) { // Moderately high-risk: mostly recent
                        daysAgo = (long)(Math.random() * 45);
                    } else if (clientType >= 3 && clientType <= 6) { // Middle-risk: mix of recent and older
                        daysAgo = (long)(Math.random() * 60);
                    } else if (clientType == 2) { // Moderately low-risk: mostly older
                        daysAgo = 45 + (long)(Math.random() * 30);
                    } else { // High-risk: very old transactions
                        daysAgo = 60 + (long)(Math.random() * 30);
                    }

                    // Calculate transaction amount with churn risk patterns
                    double baseAmount = switch (clientType) {
                        case 0, 1 -> 500.0;  // High-risk: small amounts
                        case 2 -> 1000.0;    // Moderately low-risk: moderate amounts
                        case 3, 4, 5, 6 -> 2000.0; // Middle-risk: good amounts
                        case 7 -> 3000.0;    // Moderately high-risk: large amounts
                        default -> 5000.0;   // High-value: very large amounts
                    };

                    // Modify amount based on transaction index to create churn patterns
                    double amountMultiplier = 1.0;
                    if (clientType >= 8) { // High-value: increasing amounts
                        amountMultiplier = 1.0 + (i * 0.01);
                    } else if (clientType == 7) { // Moderately high-risk: stable amounts
                        amountMultiplier = 1.0 + (Math.random() * 0.1);
                    } else if (clientType >= 3 && clientType <= 6) { // Middle-risk: slightly declining
                        amountMultiplier = 1.0 - (i * 0.005);
                    } else if (clientType == 2) { // Moderately low-risk: declining
                        amountMultiplier = 1.0 - (i * 0.01);
                    } else { // High-risk: sharply declining
                        amountMultiplier = 1.0 - (i * 0.02);
                    }

                    // Calculate failure chance with churn patterns
                    double baseFailureChance = switch (clientType) {
                        case 0, 1 -> 0.25;   // High-risk: frequent failures
                        case 2 -> 0.15;      // Moderately low-risk: some failures
                        case 3, 4, 5, 6 -> 0.10; // Middle-risk: occasional failures
                        case 7 -> 0.05;      // Moderately high-risk: rare failures
                        default -> 0.02;     // High-value: very rare failures
                    };

                    // Modify failure chance based on transaction index
                    double failureChance = baseFailureChance;
                    if (clientType <= 1) { // High-risk: increasing failure rate
                        failureChance += (i * 0.02);
                    } else if (clientType == 2) { // Moderately low-risk: slightly increasing
                        failureChance += (i * 0.01);
                    }

                    // Set balance based on churn risk
                    BigDecimal balance = senderAccount.getBalance();
                    if (clientType <= 1) { // High-risk: very low balance
                        balance = BigDecimal.valueOf(50 + Math.random() * 50);
                    } else if (clientType == 2) { // Moderately low-risk: low balance
                        balance = BigDecimal.valueOf(500 + Math.random() * 500);
                    } else if (clientType >= 3 && clientType <= 6) { // Middle-risk: moderate balance
                        balance = balance.multiply(BigDecimal.valueOf(0.95));
                    }

                    // Update account balance
                    senderAccount.setBalance(balance);
                    senderAccount.setAvailableBalance(balance);
                    accountRepository.save(senderAccount);

                    Payment payment = Payment.builder()
                            .senderName("Sender " + senderAccount.getClientId())
                            .clientId(senderAccount.getClientId())
                            .senderAccount(senderAccount)
                            .amount(BigDecimal.valueOf((Math.random() * baseAmount * amountMultiplier) + 50)
                                    .setScale(2, RoundingMode.HALF_UP))
                            .date(LocalDateTime.now().minusDays(daysAgo))
                            .status(Math.random() > failureChance ? PaymentStatus.COMPLETED : PaymentStatus.CANCELED)
                            .purposeOfPayment("Payment for service " + i)
                            .referenceNumber(String.format("REF%08d", i))
                            .accountNumberReceiver(receiverAccount.getAccountNumber())
                            .receiverClientId(receiverAccount.getClientId())
                            .build();

                    // Card usage pattern based on churn risk
                    boolean useCard = !senderAccount.getCards().isEmpty() && 
                        (clientType >= 8 || // High-value: always use card
                         clientType == 7 || // Moderately high-risk: frequent card use
                         (clientType >= 3 && clientType <= 6 && i < transactionCount/2) || // Middle-risk: declining card use
                         (clientType == 2 && Math.random() > 0.5)); // Moderately low-risk: occasional card use

                    if (useCard) {
                        payment.setCard(senderAccount.getCards().get(0));
                    }

                    if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
                        payment.setOutAmount(payment.getAmount().multiply(BigDecimal.valueOf(0.95)));
                        payment.setExchangeProfit(payment.getAmount().multiply(BigDecimal.valueOf(0.05)));
                    }

                    payment = paymentRepository.save(payment);
                    newPayments.add(payment);
                }
            }

            // Create loans with diverse characteristics
            List<Loan> newLoans = new ArrayList<>();
            List<Installment> newInstallments = new ArrayList<>();

            for (Account account : allAccounts) {
                // Skip loans for client 3 (poor credit) and client 5 (too new)
                if (account.getClientId() == 3L || account.getClientId() == 5L) {
                    continue;
                }

                // Number of loans based on client profile
                int loanCount;
                Long clientId = account.getClientId();
                
                if (clientId == 1L) {
                    loanCount = 2; // High-value client: multiple loans
                } else if (clientId == 2L) {
                    loanCount = 1; // Average client: one loan
                } else if (clientId == 4L) {
                    loanCount = (int) (Math.random() * 2); // Mixed history: 0-1 loans
                } else {
                    loanCount = 0; // Others: no loans
                }
                
                for (int j = 0; j < loanCount; j++) {
                    // Loan amount based on client profile
                    BigDecimal maxLoanAmount = account.getBalance().multiply(BigDecimal.valueOf(2));
                    BigDecimal loanAmount;
                    
                    if (clientId == 1L) {
                        loanAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.8)); // High-value client: larger loans
                    } else if (clientId == 2L) {
                        loanAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.5)); // Average client: moderate loans
                    } else {
                        loanAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.3)); // Others: smaller loans
                    }
                    
                    loanAmount = loanAmount.setScale(2, RoundingMode.HALF_UP);
                    
                    // Loan terms and rates based on client profile
                    int termMonths;
                    if (clientId == 1L) {
                        termMonths = 48 + (int) (Math.random() * 12); // 48-60 months
                    } else if (clientId == 2L) {
                        termMonths = 24 + (int) (Math.random() * 24); // 24-48 months
                    } else {
                        termMonths = 12 + (int) (Math.random() * 12); // 12-24 months
                    }
                    
                    BigDecimal monthlyRate;
                    if (clientId == 1L) {
                        monthlyRate = BigDecimal.valueOf(0.006 + (Math.random() * 0.002)); // 0.6-0.8%
                    } else if (clientId == 2L) {
                        monthlyRate = BigDecimal.valueOf(0.008 + (Math.random() * 0.002)); // 0.8-1.0%
                    } else {
                        monthlyRate = BigDecimal.valueOf(0.010 + (Math.random() * 0.002)); // 1.0-1.2%
                    }
                    
                    BigDecimal monthlyPayment = calculateMonthlyPayment(loanAmount, monthlyRate, termMonths);
                    
                    String loanNumber = "LN" + String.format("%08d", 10000000L + (j * 100));

                    // Create loan with appropriate status
                    LoanStatus status = Math.random() > 0.1 ? LoanStatus.APPROVED : LoanStatus.REJECTED;
                    if (status == LoanStatus.APPROVED) {
                        LocalDate startDate = LocalDate.now().minusMonths((int) (Math.random() * 12));
                        
                        Loan loan = Loan.builder()
                                .loanNumber(loanNumber)
                                .type(j == 0 ? LoanType.CASH : LoanType.REFINANCING)
                                .amount(loanAmount)
                                .repaymentPeriod(termMonths)
                                .nominalInterestRate(monthlyRate.multiply(BigDecimal.valueOf(12)))
                                .effectiveInterestRate(monthlyRate.multiply(BigDecimal.valueOf(12.68)))
                                .startDate(startDate)
                                .dueDate(startDate.plusMonths(termMonths))
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

                        // Create installments with payment history reflecting client profile
                        LocalDate installmentStartDate = loan.getStartDate();
                        for (int i = 0; i < termMonths; i++) {
                            LocalDate dueDate = installmentStartDate.plusMonths(i + 1);
                            
                            // Payment probability based on client profile
                            double paymentProbability;
                            if (clientId == 1L) {
                                paymentProbability = 0.95; // High-value client: excellent payment history
                            } else if (clientId == 2L) {
                                paymentProbability = 0.85; // Average client: good payment history
                            } else {
                                paymentProbability = 0.70; // Others: mixed payment history
                            }
                            
                            boolean isPaid = dueDate.isBefore(LocalDate.now()) && Math.random() < paymentProbability;
                            
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

    private void createDebitCard(Account account, BigDecimal dailyLimit, List<Card> newCards) {
        String cardNumber;
        int attempts = 0;
        do {
            cardNumber = "4" + String.format("%015d", 1000000L + account.getClientId() + attempts);
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Failed to generate unique card number after 100 attempts");
            }
        } while (cardRepository.findByCardNumber(cardNumber).isPresent());

        Card card = Card.builder()
                .cardNumber(cardNumber)
                .account(account)
                .name("Card for " + account.getName())
                .cvv("123")
                .creationDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now().plusYears(3))
                .type(CardType.DEBIT)
                .status(CardStatus.ACTIVE)
                .issuer(CardIssuer.VISA)
                .cardLimit(dailyLimit)
                .build();

        card = cardRepository.save(card);
        newCards.add(card);
    }

    private void createCreditCard(Account account, BigDecimal cardLimit, List<Card> newCards) {
        String cardNumber;
        int attempts = 0;
        do {
            cardNumber = "5" + String.format("%015d", 2000000L + account.getClientId() + attempts);
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Failed to generate unique card number after 100 attempts");
            }
        } while (cardRepository.findByCardNumber(cardNumber).isPresent());

        Card card = Card.builder()
                .cardNumber(cardNumber)
                .account(account)
                .name("Card for " + account.getName())
                .cvv("123")
                .creationDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now().plusYears(3))
                .type(CardType.CREDIT)
                .status(CardStatus.ACTIVE)
                .issuer(CardIssuer.VISA)
                .cardLimit(cardLimit)
                .build();

        card = cardRepository.save(card);
        newCards.add(card);
    }
}
