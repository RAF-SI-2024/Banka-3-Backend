package rs.raf.bank_service.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class BootstrapData implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CurrencyRepository currencyRepository;

    public BootstrapData(AccountRepository accountRepository, CardRepository cardRepository, CurrencyRepository currencyRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.currencyRepository = currencyRepository;
    }

    @Override
    public void run(String... args) {
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

        // Kreiramo račune za klijente
        PersonalAccount currentAccount1 = PersonalAccount.builder()
                .accountNumber("111111111111111111")
                .clientId(2L)
                .createdByEmployeeId(3L)
                .creationDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now().plusYears(5))
                .currency(currencyRSD)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .availableBalance(BigDecimal.valueOf(900))
                .dailyLimit(BigDecimal.valueOf(500))
                .monthlyLimit(BigDecimal.valueOf(5000))
                .dailySpending(BigDecimal.ZERO)
                .monthlySpending(BigDecimal.ZERO)
                .type(AccountType.CURRENT)
                .accountOwnerType(AccountOwnerType.PERSONAL)
                .build();

        PersonalAccount currentAccount2 = PersonalAccount.builder()
                .accountNumber("211111111111111111")
                .clientId(1L)
                .createdByEmployeeId(3L)
                .creationDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now().plusYears(5))
                .currency(currencyRSD)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .availableBalance(BigDecimal.valueOf(900))
                .dailyLimit(BigDecimal.valueOf(500))
                .monthlyLimit(BigDecimal.valueOf(5000))
                .dailySpending(BigDecimal.ZERO)
                .monthlySpending(BigDecimal.ZERO)
                .type(AccountType.CURRENT)
                .accountOwnerType(AccountOwnerType.PERSONAL)
                .build();

        CompanyAccount foreignAccount = CompanyAccount.builder()
                .accountNumber("222222222222222222")
                .clientId(1L)
                .companyId(200L)
                .createdByEmployeeId(101L)
                .creationDate(LocalDate.now().minusMonths(2))
                .expirationDate(LocalDate.now().plusYears(3))
                .currency(currencyEUR)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.valueOf(2000))
                .availableBalance(BigDecimal.valueOf(1800))
                .dailyLimit(BigDecimal.valueOf(1000))
                .monthlyLimit(BigDecimal.valueOf(10000))
                .dailySpending(BigDecimal.ZERO)
                .monthlySpending(BigDecimal.ZERO)
                .type(AccountType.FOREIGN)
                .accountOwnerType(AccountOwnerType.COMPANY)
                .build();


        // RACUNI NASE BANKE
        CompanyAccount bankAccountRSD = CompanyAccount.builder()
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

        accountRepository.saveAll(java.util.List.of(
                currentAccount1, currentAccount2, foreignAccount,
                bankAccountRSD, bankAccountEUR, bankAccountCHF, bankAccountUSD, bankAccountJPY,
                bankAccountGBP, bankAccountCAD, bankAccountAUD
        ));

        // Kreiramo kartice
        Card card1 = Card.builder()
                .cardNumber("1234123412341234")
                .cvv("123")
                .creationDate(LocalDate.now().minusDays(10))
                .expirationDate(LocalDate.now().plusYears(3))
                .account(currentAccount1)
                .status(CardStatus.ACTIVE)
                .cardLimit(BigDecimal.valueOf(500))
                .build();

        Card card2 = Card.builder()
                .cardNumber("4321432143214321")
                .cvv("321")
                .creationDate(LocalDate.now().minusDays(5))
                .expirationDate(LocalDate.now().plusYears(2))
                .account(foreignAccount)
                .status(CardStatus.ACTIVE)
                .cardLimit(BigDecimal.valueOf(1000))
                .build();

        cardRepository.saveAll(java.util.List.of(card1, card2));
    }
}
