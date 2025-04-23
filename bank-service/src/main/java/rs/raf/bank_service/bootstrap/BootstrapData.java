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
import java.util.ArrayList;
import java.util.List;

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
            // Kreiramo račune za klijente
            PersonalAccount currentAccount1 = PersonalAccount.builder()
                    .name("My RSD account")
                    .accountNumber("111111111111111111")
                    .clientId(2L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(1))
                    .expirationDate(LocalDate.now().plusYears(5))
                    .currency(currencyRSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(10000))
                    .availableBalance(BigDecimal.valueOf(10000))
                    .dailyLimit(BigDecimal.valueOf(500))
                    .monthlyLimit(BigDecimal.valueOf(5000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.CURRENT)
                    .accountOwnerType(AccountOwnerType.PERSONAL)
                    .build();

            // Kreiramo račune za klijente
            PersonalAccount foreignAccount1 = PersonalAccount.builder()
                    .name("My USD account")
                    .accountNumber("311111111111111111")
                    .clientId(2L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(1))
                    .expirationDate(LocalDate.now().plusYears(5))
                    .currency(currencyUSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(10000))
                    .availableBalance(BigDecimal.valueOf(10000))
                    .dailyLimit(BigDecimal.valueOf(500))
                    .monthlyLimit(BigDecimal.valueOf(5000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.PERSONAL)
                    .build();


            PersonalAccount currentAccount2 = PersonalAccount.builder()
                    .name("My RSD account")
                    .accountNumber("211111111111111111")
                    .clientId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(1))
                    .expirationDate(LocalDate.now().plusYears(5))
                    .currency(currencyRSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(10000))
                    .availableBalance(BigDecimal.valueOf(10000))
                    .dailyLimit(BigDecimal.valueOf(500))
                    .monthlyLimit(BigDecimal.valueOf(5000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.CURRENT)
                    .accountOwnerType(AccountOwnerType.PERSONAL)
                    .build();

            CompanyAccount foreignAccount = CompanyAccount.builder()
                    .name("My company's EUR account")
                    .accountNumber("222222222222222222")
                    .clientId(1L)
                    .companyId(3L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyEUR)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(2000))
                    .availableBalance(BigDecimal.valueOf(2000))
                    .dailyLimit(BigDecimal.valueOf(1000))
                    .monthlyLimit(BigDecimal.valueOf(10000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .authorizedPersonId(1L)
                    .build();


            // RACUNI NASE BANKE
            CompanyAccount bankAccountRSD = CompanyAccount.builder()
                    .name("Bank account for RSD")
                    .accountNumber("333000156732897612")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyRSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.CURRENT)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountEUR = CompanyAccount.builder()
                    .name("Bank account for EUR")
                    .accountNumber("333000177732897122")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyEUR)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountCHF = CompanyAccount.builder()
                    .name("Bank account for CHF")
                    .accountNumber("333000137755897822")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyCHF)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountUSD = CompanyAccount.builder()
                    .name("Bank account for USD")
                    .accountNumber("333000157555885522")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyUSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountJPY = CompanyAccount.builder()
                    .name("Bank account for JPY")
                    .accountNumber("333000117755885122")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyJPY)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountGBP = CompanyAccount.builder()
                    .name("Bank account for GBP")
                    .accountNumber("333000166675885622")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyGBP)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();


            CompanyAccount bankAccountCAD = CompanyAccount.builder()
                    .name("Bank account for CAD")
                    .accountNumber("333000188875885822")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyCAD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountAUD = CompanyAccount.builder()
                    .name("Bank account for AUD")
                    .accountNumber("333000199975899922")
                    .clientId(null)
                    .companyId(1L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyAUD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.FOREIGN)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            CompanyAccount bankAccountState = CompanyAccount.builder()
                    .name("State bank account")
                    .accountNumber("333000100000897612")
                    .clientId(null)
                    .companyId(2L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyRSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(2000000))
                    .monthlyLimit(BigDecimal.valueOf(10000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.CURRENT)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            //Racun za Berzu
            CompanyAccount stockMarketAccount = CompanyAccount.builder()
                    .name("Stock market bank account")
                    .accountNumber("333000156732897699")
                    .clientId(null)
                    .companyId(4L)
                    .createdByEmployeeId(3L)
                    .creationDate(LocalDate.now().minusMonths(2))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .currency(currencyUSD)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(50000000))
                    .availableBalance(BigDecimal.valueOf(50000000))
                    .dailyLimit(BigDecimal.valueOf(50000000))
                    .monthlyLimit(BigDecimal.valueOf(50000000))
                    .dailySpending(BigDecimal.ZERO)
                    .monthlySpending(BigDecimal.ZERO)
                    .type(AccountType.CURRENT)
                    .accountOwnerType(AccountOwnerType.COMPANY)
                    .build();

            accountRepository.saveAll(java.util.List.of(
                    currentAccount1, currentAccount2, foreignAccount, foreignAccount1,
                    bankAccountRSD, bankAccountEUR, bankAccountCHF, bankAccountUSD, bankAccountJPY,
                    bankAccountGBP, bankAccountCAD, bankAccountAUD, bankAccountState, stockMarketAccount
            ));

            // Kreiramo kartice
            Card card1 = Card.builder()
                    .name("Moja debitna")
                    .cardNumber("1234123412341234")
                    .cvv("123")
                    .creationDate(LocalDate.now().minusDays(10))
                    .expirationDate(LocalDate.now().plusYears(3))
                    .account(currentAccount1)
                    .status(CardStatus.ACTIVE)
                    .cardLimit(BigDecimal.valueOf(500))
                    .issuer(CardIssuer.VISA)
                    .type(CardType.DEBIT)
                    .build();

            Card card2 = Card.builder()
                    .name("Moja kreditna")
                    .cardNumber("4321432143214321")
                    .cvv("321")
                    .creationDate(LocalDate.now().minusDays(5))
                    .expirationDate(LocalDate.now().plusYears(2))
                    .account(foreignAccount)
                    .status(CardStatus.ACTIVE)
                    .cardLimit(BigDecimal.valueOf(1000))
                    .issuer(CardIssuer.VISA)
                    .type(CardType.CREDIT)
                    .build();

            cardRepository.saveAll(java.util.List.of(card1, card2));

            // Kreiranje primera zahteva za kredit
            LoanRequest loanRequest = LoanRequest.builder()
                    .type(LoanType.AUTO)
                    .amount(new BigDecimal("500000"))
                    .purpose("Kupovina automobila")
                    .monthlyIncome(new BigDecimal("1000"))
                    .employmentStatus(EmploymentStatus.PERMANENT)
                    .employmentDuration(36)
                    .repaymentPeriod(24)
                    .contactPhone("+381641234567")
                    .account(currentAccount1)
                    .currency(currencyRSD)
                    .status(LoanRequestStatus.APPROVED)
                    .interestRateType(InterestRateType.FIXED)
                    .build();

            LoanRequest loanRequest2 = LoanRequest.builder()
                    .type(LoanType.CASH)
                    .amount(new BigDecimal("300000"))
                    .purpose("Kupovina necega")
                    .monthlyIncome(new BigDecimal("1000"))
                    .employmentStatus(EmploymentStatus.PERMANENT)
                    .employmentDuration(36)
                    .repaymentPeriod(24)
                    .contactPhone("+381641234567")
                    .account(currentAccount1)
                    .currency(currencyRSD)
                    .status(LoanRequestStatus.PENDING)
                    .interestRateType(InterestRateType.FIXED)
                    .build();

            loanRequestRepository.save(loanRequest);
            loanRequestRepository.save(loanRequest2);

            // Kreiranje primera odobrenog kredita
            Loan loan = Loan.builder()
                    .loanNumber("d7742918-4b78-44eb-93b7-25adfd5123e9")
                    .type(LoanType.AUTO)
                    .amount(new BigDecimal("500000"))
                    .repaymentPeriod(24)
                    .nominalInterestRate(new BigDecimal("5.5"))
                    .effectiveInterestRate(new BigDecimal("6.0"))
                    .startDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusMonths(24))
                    .nextInstallmentAmount(new BigDecimal("220"))
                    .nextInstallmentDate(LocalDate.now().plusMonths(1))
                    .remainingDebt(new BigDecimal("500000"))
                    .currency(currencyRSD)
                    .status(LoanStatus.APPROVED)
                    .interestRateType(InterestRateType.FIXED)
                    .account(currentAccount1)
                    .build();

            loanRepository.save(loan);

            Installment installment = Installment.builder()
                    .amount(new BigDecimal("220"))
                    .installmentStatus(InstallmentStatus.UNPAID)
                    .loan(loan)
                    .expectedDueDate(LocalDate.now().plusMonths(1))
                    .interestRate(new BigDecimal("6.0"))
                    .build();

            installmentRepository.save(installment);
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
}
