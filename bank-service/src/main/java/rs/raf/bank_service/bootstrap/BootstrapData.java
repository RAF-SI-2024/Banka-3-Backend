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

            // Client profiles with natural variations:
            // - Mix of account balances (very low to very high)
            // - Various transaction patterns (frequency, amounts, success rates)
            // - Different card usage behaviors (no cards, debit only, credit, multiple cards)
            // - Mix of activity levels and transaction types
            // Let natural segments emerge from data

            for (int i = 1; i <= 20; i++) {
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

                // Create naturally diverse base characteristics
                double profileRandomizer = Math.random(); // Used for creating natural groupings
                double activityLevel = Math.random(); // General activity level
                double riskLevel = Math.random(); // Risk level affects decline rates and behaviors

                // Set base values with high variability
                BigDecimal baseBalance;
                BigDecimal baseLimit;

                // Determine base balance - create natural variety
                if (profileRandomizer < 0.15) { // Very low balance accounts
                    baseBalance = BigDecimal.valueOf(100 + Math.random() * 900); // 100-1000
                    baseLimit = BigDecimal.valueOf(200);
                    accountType = AccountType.CURRENT;
                } else if (profileRandomizer < 0.40) { // Low-moderate balance
                    baseBalance = BigDecimal.valueOf(1000 + Math.random() * 4000); // 1000-5000
                    baseLimit = BigDecimal.valueOf(1000);
                    accountType = AccountType.CURRENT;
                } else if (profileRandomizer < 0.70) { // Moderate balance
                    baseBalance = BigDecimal.valueOf(5000 + Math.random() * 15000); // 5000-20000
                    baseLimit = BigDecimal.valueOf(2000);
                    accountType = AccountType.CURRENT;
                } else if (profileRandomizer < 0.85) { // High balance
                    baseBalance = BigDecimal.valueOf(20000 + Math.random() * 30000); // 20000-50000
                    baseLimit = BigDecimal.valueOf(5000);
                    accountType = Math.random() > 0.5 ? AccountType.FOREIGN : AccountType.CURRENT;
                } else { // Very high balance
                    baseBalance = BigDecimal.valueOf(50000 + Math.random() * 50000); // 50000-100000
                    baseLimit = BigDecimal.valueOf(10000);
                    accountType = AccountType.FOREIGN;
                }

                // Add some randomization to limits
                double limitVariation = 0.75 + (Math.random() * 0.5); // 75%-125% of base limit
                dailyLimit = baseLimit.multiply(BigDecimal.valueOf(limitVariation));
                monthlyLimit = dailyLimit.multiply(BigDecimal.valueOf(15 + Math.random() * 10)); // 15-25x daily limit

                // Actual balance varies from base
                double balanceVariation = 0.8 + (Math.random() * 0.4); // 80%-120% of base balance
                balance = baseBalance.multiply(BigDecimal.valueOf(balanceVariation));

                // Create account with diverse characteristics
                Account account = PersonalAccount.builder()
                        .name("Client " + i + " Account")
                        .accountNumber(accountNumber)
                        .clientId((long) i)
                        .createdByEmployeeId(3L)
                        .creationDate(LocalDate.now().minusMonths((int)(Math.random() * 24))) // Random age 0-24 months
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

                // Card creation with natural variety
                double cardChance = Math.random();
                if (cardChance < 0.2) { // 20% no cards
                    // No cards for this client
                } else if (cardChance < 0.5) { // 30% debit only
                    createDebitCard(account, dailyLimit, newCards);
                } else if (cardChance < 0.8) { // 30% debit + credit
                    createDebitCard(account, dailyLimit, newCards);
                    createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(1.5 + Math.random())), newCards);
                } else { // 20% multiple cards
                    createDebitCard(account, dailyLimit, newCards);
                    createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(2 + Math.random())), newCards);
                    if (Math.random() > 0.5) { // Sometimes a second credit card
                        createCreditCard(account, dailyLimit.multiply(BigDecimal.valueOf(1.5 + Math.random())), newCards);
                    }
                }

                // Create second account for some clients (independent of balance)
                if (Math.random() < 0.3) { // 30% chance of second account
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

            // Create diverse transactions
            List<Account> allAccounts = accountRepository.findAll();
            for (Account senderAccount : allAccounts) {
                // Determine transaction patterns
                double activityLevel = Math.random(); // Overall activity level
                double consistencyLevel = Math.random(); // How consistent are their transactions
                double riskLevel = Math.random(); // Affects failure rates

                // Base number of transactions varies greatly
                int baseTransactions;
                if (activityLevel < 0.2) { // Very low activity
                    baseTransactions = 1 + (int)(Math.random() * 5); // 1-5 transactions
                } else if (activityLevel < 0.5) { // Low activity
                    baseTransactions = 5 + (int)(Math.random() * 10); // 5-15 transactions
                } else if (activityLevel < 0.8) { // Moderate activity
                    baseTransactions = 15 + (int)(Math.random() * 20); // 15-35 transactions
                } else { // High activity
                    baseTransactions = 35 + (int)(Math.random() * 30); // 35-65 transactions
                }

                for (int i = 0; i < baseTransactions; i++) {
                    // Select random receiver
                    Account receiverAccount;
                    do {
                        receiverAccount = allAccounts.get((int)(Math.random() * allAccounts.size()));
                    } while (receiverAccount.getAccountNumber().equals(senderAccount.getAccountNumber()));

                    // Transaction timing varies by consistency level
                    long daysAgo;
                    if (consistencyLevel < 0.3) { // Irregular activity
                        daysAgo = (long)(Math.random() * 90); // 0-90 days
                    } else if (consistencyLevel < 0.7) { // Somewhat regular
                        daysAgo = (long)(Math.random() * 45); // 0-45 days
                    } else { // Regular activity
                        daysAgo = (long)(Math.random() * 30); // 0-30 days
                    }

                    // Transaction amounts vary by account balance and activity pattern
                    double baseAmount;
                    double randomFactor = Math.random();
                    if (randomFactor < 0.6) { // 60% normal transactions
                        baseAmount = 50 + Math.random() * 950; // 50-1000
                    } else if (randomFactor < 0.9) { // 30% larger transactions
                        baseAmount = 1000 + Math.random() * 4000; // 1000-5000
                    } else { // 10% very large transactions
                        baseAmount = 5000 + Math.random() * 15000; // 5000-20000
                    }

                    // Adjust amount based on account balance (can't send more than they have)
                    baseAmount = Math.min(baseAmount, senderAccount.getBalance().doubleValue() * 0.9);

                    // Calculate failure chance based on risk level and amount
                    double failureChance = 0.05 + (riskLevel * 0.15); // Base 5-20% failure rate
                    failureChance += (baseAmount / senderAccount.getBalance().doubleValue()) * 0.1; // Higher chance for larger relative amounts

                    // Create payment with natural variations
                    Payment payment = Payment.builder()
                            .senderName("Sender " + senderAccount.getClientId())
                            .clientId(senderAccount.getClientId())
                            .senderAccount(senderAccount)
                            .amount(BigDecimal.valueOf(baseAmount).setScale(2, RoundingMode.HALF_UP))
                            .date(LocalDateTime.now().minusDays(daysAgo))
                            .status(Math.random() > failureChance ? PaymentStatus.COMPLETED : PaymentStatus.CANCELED)
                            .purposeOfPayment("Payment " + i)
                            .referenceNumber(String.format("REF%08d", i))
                            .accountNumberReceiver(receiverAccount.getAccountNumber())
                            .receiverClientId(receiverAccount.getClientId())
                            .build();

                    // Card usage varies by availability and preference
                    if (!senderAccount.getCards().isEmpty() && Math.random() > 0.3) { // 70% card usage if available
                        payment.setCard(senderAccount.getCards().get(0));
                    }

                    // Handle currency exchange if needed
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
                // Determine loan eligibility and patterns
                double creditScore = Math.random(); // Random credit score for variety
                double paymentReliability = Math.random(); // How reliable are they with payments
                double riskAppetite = Math.random(); // How much they tend to borrow

                // Number of loan applications (not all will be approved)
                int loanApplications;
                if (riskAppetite < 0.4) { // Conservative borrowers
                    loanApplications = 1;
                } else if (riskAppetite < 0.7) { // Moderate borrowers
                    loanApplications = 1 + (int)(Math.random() * 2); // 1-2 loans
                } else if (riskAppetite < 0.9) { // Active borrowers
                    loanApplications = 2 + (int)(Math.random() * 2); // 2-3 loans
                } else { // Heavy borrowers
                    loanApplications = 3 + (int)(Math.random() * 2); // 3-4 loans
                }

                for (int j = 0; j < loanApplications; j++) {
                    // Loan amount based on account balance and credit score
                    BigDecimal maxLoanAmount = account.getBalance().multiply(BigDecimal.valueOf(3)); // Can borrow up to 3x balance
                    BigDecimal requestedAmount;

                    if (riskAppetite < 0.3) { // Conservative
                        requestedAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.2 + Math.random() * 0.3)); // 20-50% of max
                    } else if (riskAppetite < 0.7) { // Moderate
                        requestedAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.4 + Math.random() * 0.3)); // 40-70% of max
                    } else { // Aggressive
                        requestedAmount = maxLoanAmount.multiply(BigDecimal.valueOf(0.6 + Math.random() * 0.4)); // 60-100% of max
                    }

                    // Round to 2 decimal places
                    requestedAmount = requestedAmount.setScale(2, RoundingMode.HALF_UP);

                    // Determine loan approval based on multiple factors
                    double approvalChance = creditScore; // Base approval on credit score
                    approvalChance -= (requestedAmount.doubleValue() / maxLoanAmount.doubleValue()) * 0.2; // Larger loans are riskier
                    approvalChance -= (j * 0.1); // Each additional loan reduces approval chance
                    approvalChance += (account.getBalance().doubleValue() / 10000) * 0.1; // Higher balance helps
                    
                    // Loan terms based on amount and credit score
                    int termMonths;
                    if (requestedAmount.compareTo(BigDecimal.valueOf(10000)) < 0) {
                        termMonths = 12 + (int)(Math.random() * 12); // 12-24 months for small loans
                    } else if (requestedAmount.compareTo(BigDecimal.valueOf(50000)) < 0) {
                        termMonths = 24 + (int)(Math.random() * 24); // 24-48 months for medium loans
                    } else {
                        termMonths = 48 + (int)(Math.random() * 36); // 48-84 months for large loans
                    }

                    // Interest rate based on credit score and loan size
                    BigDecimal baseRate = BigDecimal.valueOf(0.05); // 5% base rate
                    baseRate = baseRate.add(BigDecimal.valueOf((1 - creditScore) * 0.05)); // Add up to 5% for low credit
                    baseRate = baseRate.add(BigDecimal.valueOf(requestedAmount.doubleValue() / maxLoanAmount.doubleValue() * 0.02)); // Add up to 2% for loan size
                    BigDecimal monthlyRate = baseRate.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

                    // Calculate monthly payment
                    BigDecimal monthlyPayment = calculateMonthlyPayment(requestedAmount, monthlyRate, termMonths);

                    // Determine loan type with some variety
                    LoanType loanType;
                    double loanTypeRandom = Math.random();
                    if (loanTypeRandom < 0.4) {
                        loanType = LoanType.CASH;
                    } else if (loanTypeRandom < 0.7) {
                        loanType = LoanType.REFINANCING;
                    } else {
                        loanType = LoanType.MORTGAGE;
                    }

                    // Create loan with appropriate status
                    LoanStatus status = Math.random() < approvalChance ? LoanStatus.APPROVED : LoanStatus.REJECTED;
                    
                    if (status == LoanStatus.APPROVED) {
                        LocalDate startDate = LocalDate.now().minusMonths((int)(Math.random() * 12));
                        
                        Loan loan = Loan.builder()
                                .loanNumber("LN" + String.format("%08d", 10000000L + (account.getClientId() * 100) + j))
                                .type(loanType)
                                .amount(requestedAmount)
                                .repaymentPeriod(termMonths)
                                .nominalInterestRate(baseRate)
                                .effectiveInterestRate(baseRate.multiply(BigDecimal.valueOf(1.2)))
                                .startDate(startDate)
                                .dueDate(startDate.plusMonths(termMonths))
                                .nextInstallmentAmount(monthlyPayment)
                                .nextInstallmentDate(LocalDate.now().plusMonths(1))
                                .remainingDebt(requestedAmount)
                                .currency(account.getCurrency())
                                .status(status)
                                .interestRateType(Math.random() > 0.7 ? InterestRateType.VARIABLE : InterestRateType.FIXED)
                                .account(account)
                                .build();

                        loan = loanRepository.save(loan);
                        newLoans.add(loan);

                        // Create installments with varied payment history
                        LocalDate installmentDate = loan.getStartDate();
                        BigDecimal remainingDebt = loan.getAmount();
                        
                        for (int i = 0; i < termMonths; i++) {
                            LocalDate dueDate = installmentDate.plusMonths(i + 1);
                            boolean isPastDue = dueDate.isBefore(LocalDate.now());
                            
                            // Payment probability based on multiple factors
                            double paymentProbability = paymentReliability; // Base on general reliability
                            paymentProbability -= (1 - creditScore) * 0.2; // Lower credit score means more missed payments
                            paymentProbability -= (i * 0.01); // Slight decrease over time
                            paymentProbability -= (monthlyPayment.doubleValue() / account.getBalance().doubleValue()) * 0.2; // Higher payments relative to balance are harder to make
                            
                            // Add some randomness to payment timing
                            LocalDate actualPaymentDate = null;
                            if (isPastDue && Math.random() < paymentProbability) {
                                int daysLate = (int)(Math.random() * 15); // 0-15 days late
                                if (Math.random() < 0.1) { // 10% chance of being very late
                                    daysLate += (int)(Math.random() * 30); // Additional 0-30 days
                                }
                                actualPaymentDate = dueDate.plusDays(daysLate);
                            }

                            InstallmentStatus installmentStatus;
                            if (!isPastDue) {
                                installmentStatus = InstallmentStatus.UNPAID;
                            } else if (actualPaymentDate != null) {
                                installmentStatus = InstallmentStatus.PAID;
                                remainingDebt = remainingDebt.subtract(monthlyPayment);
                            } else {
                                installmentStatus = InstallmentStatus.LATE;
                            }

                            Installment installment = Installment.builder()
                                    .loan(loan)
                                    .amount(monthlyPayment)
//                                    .remainingDebt(remainingDebt)
                                    .actualDueDate(dueDate)
                                    .installmentStatus(installmentStatus)
                                    .expectedDueDate(actualPaymentDate)
                                    .build();

                            installment = installmentRepository.save(installment);
                            newInstallments.add(installment);
                        }

                        // Update loan's remaining debt
                        loan.setRemainingDebt(remainingDebt);
                        loanRepository.save(loan);
                    } else {
                        // Create rejected loan record
                        Loan rejectedLoan = Loan.builder()
                                .loanNumber("LN" + String.format("%08d", 90000000L + (account.getClientId() * 100) + j))
                                .type(loanType)
                                .amount(requestedAmount)
                                .repaymentPeriod(termMonths)
                                .nominalInterestRate(baseRate)
                                .effectiveInterestRate(baseRate.multiply(BigDecimal.valueOf(1.2)))
                                .startDate(LocalDate.now())
                                .dueDate(LocalDate.now().plusMonths(termMonths))
                                .currency(account.getCurrency())
                                .status(LoanStatus.REJECTED)
                                .interestRateType(Math.random() > 0.7 ? InterestRateType.VARIABLE : InterestRateType.FIXED)
                                .account(account)
                                .build();

                        rejectedLoan = loanRepository.save(rejectedLoan);
                        newLoans.add(rejectedLoan);
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
