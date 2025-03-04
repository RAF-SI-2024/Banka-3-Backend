package rs.raf.bank_service.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.AccountStatus;
import rs.raf.bank_service.domain.enums.AccountType;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.repository.CurrencyRepository;

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
    public void run(String... args) throws Exception {
        // Kreiramo valutu
        Currency currency = new Currency();
        currency.setCode("EUR");
        currency.setName("Euro");
        currency.setSymbol("€");
        currency.setCountries("EU");
        currency.setDescription("Euro currency");
        currency.setActive(true);
        currencyRepository.save(currency);

        // Postavka računa (podaci za bank service koriste ID-jeve kreiranih klijenata u client service):
        // Pretpostavljamo da je prvi klijent (Marko Markovic) sa ID 1, a drugi (Jovan Jovanovic) sa ID 2.

        // Tekući račun (lični) – za klijenta sa ID 2
        PersonalAccount currentAccount = new PersonalAccount();
        currentAccount.setAccountNumber("111111111111111111");
        currentAccount.setClientId(2L);             // Klijent: Marko Markovic (lični)
        currentAccount.setCreatedByEmployeeId(100L);
        currentAccount.setCreationDate(LocalDate.now().minusMonths(1));
        currentAccount.setExpirationDate(LocalDate.now().plusYears(5));
        currentAccount.setCurrency(currency);
        currentAccount.setStatus(AccountStatus.ACTIVE);
        currentAccount.setBalance(BigDecimal.valueOf(1000));
        currentAccount.setAvailableBalance(BigDecimal.valueOf(900));
        currentAccount.setDailyLimit(BigDecimal.valueOf(500));
        currentAccount.setMonthlyLimit(BigDecimal.valueOf(5000));
        currentAccount.setDailySpending(BigDecimal.ZERO);
        currentAccount.setMonthlySpending(BigDecimal.ZERO);
        currentAccount.setType(AccountType.CURRENT);
        currentAccount.setAccountOwnerType(AccountOwnerType.PERSONAL);
        accountRepository.save(currentAccount);

        // Devizni račun (poslovni) – za klijenta sa ID 1
        CompanyAccount foreignAccount = new CompanyAccount();
        foreignAccount.setAccountNumber("222222222222222222");
        foreignAccount.setClientId(1L);              // Klijent: Jovan Jovanovic (poslovni)
        foreignAccount.setCompanyId(200L);           // Poslovni račun, ima companyId
        foreignAccount.setCreatedByEmployeeId(101L);
        foreignAccount.setCreationDate(LocalDate.now().minusMonths(2));
        foreignAccount.setExpirationDate(LocalDate.now().plusYears(3));
        foreignAccount.setCurrency(currency);
        foreignAccount.setStatus(AccountStatus.ACTIVE);
        foreignAccount.setBalance(BigDecimal.valueOf(2000));
        foreignAccount.setAvailableBalance(BigDecimal.valueOf(1800));
        foreignAccount.setDailyLimit(BigDecimal.valueOf(1000));
        foreignAccount.setMonthlyLimit(BigDecimal.valueOf(10000));
        foreignAccount.setDailySpending(BigDecimal.ZERO);
        foreignAccount.setMonthlySpending(BigDecimal.ZERO);
        foreignAccount.setType(AccountType.FOREIGN);
        foreignAccount.setAccountOwnerType(AccountOwnerType.COMPANY);
        accountRepository.save(foreignAccount);

        // Kreiramo karticu za tekući račun (Marko Markovic)
        Card card1 = new Card();
        card1.setCardNumber("1234123412341234");
        card1.setCvv("123");
        card1.setCreationDate(LocalDate.now().minusDays(10));
        card1.setExpirationDate(LocalDate.now().plusYears(3));
        card1.setAccount(currentAccount);
        card1.setStatus(CardStatus.ACTIVE);
        card1.setCardLimit(BigDecimal.valueOf(500));
        cardRepository.save(card1);

        // Kreiramo karticu za devizni račun (Jovan Jovanovic)
        Card card2 = new Card();
        card2.setCardNumber("4321432143214321");
        card2.setCvv("321");
        card2.setCreationDate(LocalDate.now().minusDays(5));
        card2.setExpirationDate(LocalDate.now().plusYears(2));
        card2.setAccount(foreignAccount);
        card2.setStatus(CardStatus.ACTIVE);
        card2.setCardLimit(BigDecimal.valueOf(1000));
        cardRepository.save(card2);
    }
}