package rs.raf.bank_service.service;

import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.domain.mapper.CardMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    UserService userService;
    AccountMapper accountMapper;
    AccountRepository accountRepository;

    public static String generateCVV() {
        Random random = new Random();
        int cvv = 100 + random.nextInt(900);
        return String.valueOf(cvv);
    }

    private boolean isBusiness(AccountTypeDto accountTypeDto) {
        return accountTypeDto.getSubtype().equals("Company");
    }

    public CardDtoNoOwner createCard(CreateCardDto createCardDto) {
        Account account = accountRepository.findByAccountNumber(createCardDto.getAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("Account with account number: " + createCardDto.getAccountNumber() + " not found"));
        AccountTypeDto accountTypeDto = accountMapper.toAccountTypeDto(account);

        Long cardCount = cardRepository.countByAccount(account);

        if ((isBusiness(accountTypeDto) && cardCount > 0) || (!isBusiness(accountTypeDto) && cardCount > 1)) {
            throw new CardLimitExceededException(accountTypeDto.getAccountNumber());
        }
        if (createCardDto.getCardLimit() != null && createCardDto.getCardLimit().compareTo(BigDecimal.ZERO) <= 0) {
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

        return CardMapper.toCardDtoNoOwner(card);
    }

    public void requestCardForAccount(CreateCardDto createCardDto) {
        Account account = accountRepository.findByAccountNumber(createCardDto.getAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("Account with account number: " + createCardDto.getAccountNumber() + " not found"));
        AccountTypeDto accountTypeDto = accountMapper.toAccountTypeDto(account);

        Long cardCount = cardRepository.countByAccount(account);

        if ((isBusiness(accountTypeDto) && cardCount > 0) || (!isBusiness(accountTypeDto) && cardCount > 1)) {
            throw new CardLimitExceededException(accountTypeDto.getAccountNumber());
        }
        if (createCardDto.getCardLimit() != null && createCardDto.getCardLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCardLimitException();
        }

        ClientDto client;
        try {
            client = userClient.getClientById(account.getClientId());
        } catch (FeignException.NotFound e) {
            throw new ClientNotFoundException(account.getClientId());
        } catch (Exception e) {
            throw new ExternalServiceException();
        }

        String email = client.getEmail();
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email address not found for user.");
        }

        try {
            userClient.requestCard(new RequestCardDto(client.getEmail()));
        } catch (FeignException e) {
            throw new ExternalServiceException();
        }
    }

    public CardDtoNoOwner recieveCardForAccount(String token, CreateCardDto createCardDto) {
        try {
            userClient.checkToken(new CheckTokenDto(token));
        } catch (FeignException.NotFound e) {
            throw new InvalidTokenException();
        } catch (Exception e) {
            throw new ExternalServiceException();
        }

        return createCard(createCardDto);
    }

    private String generateCardNumber(String name) {
        String firstFifteen = generateMIIandIIN(name) + generateAccountNumber();
        return firstFifteen + luhnDigit(firstFifteen);
    }

    private String generateMIIandIIN(String name) {
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

    private String generateAccountNumber() {
        Random random = new Random();

        int accountNumber = random.nextInt(1000000000);
        return String.format("%09d", accountNumber);
    }


    private String luhnDigit(String firstFifteen) {
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

    @Operation(
            summary = "Retrieve cards by account",
            description = "Returns a list of card DTOs associated with the provided account number."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No cards found for the given account")
    })
    public List<CardDto> getCardsByAccount(
            @Parameter(description = "Account number to search for", example = "222222222222222222")
            String accountNumber) {
        List<Card> cards = cardRepository.findByAccount_AccountNumber(accountNumber);
        return cards.stream().map(card -> {
            ClientDto client = userClient.getClientById(card.getAccount().getClientId());
            return rs.raf.bank_service.domain.mapper.CardMapper.toDto(card, client);
        }).collect(Collectors.toList());
    }

    @Operation(
            summary = "Change card status",
            description = "Changes the status of a card identified by its card number and sends an email notification to the card owner."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card status updated and notification sent successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public void changeCardStatus(
            @Parameter(description = "Card number", example = "1234123412341234")
            String cardNumber,
            @Parameter(description = "New status for the card", example = "BLOCKED")
            CardStatus newStatus) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with number: " + cardNumber));

        card.setStatus(newStatus);
        cardRepository.save(card);

        ClientDto owner = userClient.getClientById(card.getAccount().getClientId());

        EmailRequestDto emailRequestDto = new EmailRequestDto();
        emailRequestDto.setCode(newStatus.toString());
        emailRequestDto.setDestination(owner.getEmail());

        rabbitTemplate.convertAndSend("card-status-change", emailRequestDto);
    }


}
