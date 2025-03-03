package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.AccountTypeDto;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.AccountStatus;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;
import rs.raf.bank_service.exceptions.CardLimitExceededException;
import rs.raf.bank_service.exceptions.InvalidCardLimitException;
import rs.raf.bank_service.exceptions.InvalidCardTypeException;
import rs.raf.bank_service.mapper.AccountMapper;
import rs.raf.bank_service.mapper.CardMapper;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Service
@AllArgsConstructor
public class CardService {

    CardRepository cardRepository;
    AccountMapper accountMapper;
    AccountRepository accountRepository;

    private boolean isBusiness(AccountTypeDto accountTypeDto){
        return accountTypeDto.getSubtype().equals("Company");
    }

    public CardDto createCard(CreateCardDto createCardDto){
        Account account = accountRepository.findByAccountNumber(createCardDto.getAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("Account with account number: " + createCardDto.getAccountNumber() + " not found"));
        AccountTypeDto accountTypeDto = accountMapper.toAccountTypeDto(account);

        Long cardCount = cardRepository.countByAccount(account);

        if ((isBusiness(accountTypeDto) && cardCount>0) || (!isBusiness(accountTypeDto) && cardCount>1)){
            throw new CardLimitExceededException(accountTypeDto.getAccountNumber());
        }
        if (createCardDto.getCardLimit() != null && createCardDto.getCardLimit().compareTo(BigDecimal.ZERO) <= 0){
            throw new InvalidCardLimitException();
        }
        CardType cardType;
        try {
            cardType = CardType.valueOf(createCardDto.getType());
        } catch (IllegalArgumentException e) {
            throw new InvalidCardTypeException();
        }

        Card card = new Card();

        card.setCreationDate(LocalDate.now());
        card.setExpirationDate(LocalDate.now().plusMonths(60));

        card.setCardNumber(generateCardNumber(createCardDto.getName()));
        card.setCvv(generateCVV());
        card.setType(cardType);
        card.setName(createCardDto.getName());
        card.setAccount(account);
        card.setStatus(CardStatus.ACTIVE);
        card.setCardLimit(createCardDto.getCardLimit());

        cardRepository.save(card);

        return CardMapper.toCardDto(card);
    }

    private String generateCardNumber(String name){
        String firstFifteen = generateMIIandIIN(name) + generateAccountNumber();
        return firstFifteen + luhnDigit(firstFifteen);
    }

    public static String generateCVV() {
        Random random = new Random();
        int cvv = 100 + random.nextInt(900);
        return String.valueOf(cvv);
    }

    private String generateMIIandIIN(String name){
        Random random = new Random();

        switch (name.toLowerCase()) {
            case "visa":
                return "433333";
            case "mastercard":
                if (random.nextBoolean()) {
                    return 51 + random.nextInt(5) + "3333";
                } else {
                    return 2221 + random.nextInt(500) + "33";
                }
            case "dinacard":
                return "989133";
            case "american_express":
                if (random.nextBoolean()) {
                    return "343333";
                } else {
                    return "373333";
                }
            default:
                throw new IllegalArgumentException("Unsupported card type");
        }
    }

    private String generateAccountNumber(){
        Random random = new Random();

        int accountNumber = random.nextInt(1000000000);
        return String.format("%09d", accountNumber);
    }


    private String luhnDigit(String firstFifteen){
        int sum = 0;
        boolean shouldDouble = true;

        for (int i = firstFifteen.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(firstFifteen.charAt(i));

            if (shouldDouble) {
                digit = digit * 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum = sum + digit;
            shouldDouble = !shouldDouble;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return String.valueOf(checkDigit);
    }


}
