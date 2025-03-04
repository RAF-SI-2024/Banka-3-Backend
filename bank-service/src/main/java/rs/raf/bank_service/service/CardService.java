package rs.raf.bank_service.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.mapper.CardMapper;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;

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
        private final AccountRepository accountRepository;
        private final UserClient userClient;
        private final RabbitTemplate rabbitTemplate;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Operation(summary = "Retrieve cards by account", description = "Returns a list of card DTOs associated with the provided account number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "No cards found for the given account")
        })
        public List<CardDto> getCardsByAccount(
                        @Parameter(description = "Account number to search for", example = "222222222222222222") String accountNumber) {
                List<Card> cards = cardRepository.findByAccount_AccountNumber(accountNumber);
                return cards.stream().map(card -> {
                        ClientDto client = userClient.getClientById(card.getAccount().getClientId());
                        return CardMapper.toDto(card, client);
                }).collect(Collectors.toList());
        }

        @Operation(summary = "Change card status", description = "Changes the status of a card identified by its card number and sends an email notification to the card owner.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card status updated and notification sent successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found")
        })
        public void changeCardStatus(
                        @Parameter(description = "Card number", example = "1234123412341234") String cardNumber,
                        @Parameter(description = "New status for the card", example = "BLOCKED") CardStatus newStatus) {
                Card card = cardRepository.findByCardNumber(cardNumber)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Card not found with number: " + cardNumber));

                card.setStatus(newStatus);
                cardRepository.save(card);

                ClientDto owner = userClient.getClientById(card.getAccount().getClientId());

                EmailRequestDto emailRequestDto = new EmailRequestDto();
                emailRequestDto.setCode(newStatus.toString());
                emailRequestDto.setDestination(owner.getEmail());

                rabbitTemplate.convertAndSend("card-status-change", emailRequestDto);
        }

        public CardDto createPersonalCard(CreatePersonalCardDto createPersonalCardDto, String authHeader) {
                // Extract user ID from JWT
                String token = authHeader.substring(7); // Remove "Bearer "
                Claims claims = jwtAuthenticationFilter.getClaimsFromToken(token);
                Long currentUserId = claims.get("userId", Long.class);

                // Find account and verify ownership
                Account account = accountRepository.findByAccountNumber(createPersonalCardDto.getAccountNumber())
                                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

                if (!account.getClientId().equals(currentUserId)) {
                        throw new UnauthorizedException("You can only create cards for your own accounts");
                }

                if (account.getAccountOwnerType() != AccountOwnerType.PERSONAL) {
                        throw new UnauthorizedException("This endpoint is only for personal accounts");
                }

                // Create and save card
                Card card = createNewCard(account, createPersonalCardDto.getCardLimit());
                card = cardRepository.save(card);

                // Send email notification
                ClientDto owner = userClient.getClientById(account.getClientId());
                sendCardCreationEmail(owner.getEmail());

                return CardMapper.toDto(card, owner);
        }

        public CardDto createCompanyCard(CreateCompanyCardDto createCompanyCardDto, String authHeader) {
                // Extract user ID from JWT
                String token = authHeader.substring(7); // Remove "Bearer "
                Claims claims = jwtAuthenticationFilter.getClaimsFromToken(token);
                Long currentUserId = claims.get("userId", Long.class);

                // Find account
                Account account = accountRepository.findByAccountNumber(createCompanyCardDto.getAccountNumber())
                                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

                if (account.getAccountOwnerType() != AccountOwnerType.COMPANY) {
                        throw new UnauthorizedException("This endpoint is only for company accounts");
                }

                // Get company ID from CompanyAccount
                if (!(account instanceof CompanyAccount)) {
                        throw new IllegalStateException(
                                        "Account is marked as COMPANY but is not a CompanyAccount instance");
                }
                CompanyAccount companyAccount = (CompanyAccount) account;
                Long companyId = companyAccount.getCompanyId();

                // Get company details and verify majority owner
                CompanyDto company = userClient.getCompanyById(companyId);
                if (!company.getMajorityOwner().getId().equals(currentUserId)) {
                        throw new UnauthorizedException("Only the majority owner can create company cards");
                }

                ClientDto cardHolder;
                if (createCompanyCardDto.isForOwner()) {
                        // Card is for the owner
                        cardHolder = company.getMajorityOwner();
                        if (createCompanyCardDto.getAuthorizedPersonnelId() != null) {
                                throw new IllegalArgumentException(
                                                "Cannot specify authorized personnel when creating card for owner");
                        }
                } else {
                        // Card is for authorized personnel
                        if (createCompanyCardDto.getAuthorizedPersonnelId() == null) {
                                throw new IllegalArgumentException(
                                                "Authorized personnel ID is required when not creating card for owner");
                        }
                        // Verify authorized personnel exists and belongs to company
                        cardHolder = userClient.getClientById(createCompanyCardDto.getAuthorizedPersonnelId());
                        List<AuthorizedPersonelDto> authorizedPersonnel = userClient
                                        .getAuthorizedPersonnelByCompany(company.getId());
                        boolean isAuthorized = authorizedPersonnel.stream()
                                        .anyMatch(ap -> ap.getId().equals(cardHolder.getId()));
                        if (!isAuthorized) {
                                throw new UnauthorizedException("Selected person is not authorized for this company");
                        }
                }

                // Create and save card
                Card card = createNewCard(account, createCompanyCardDto.getCardLimit());
                card = cardRepository.save(card);

                // Send email notification
                sendCardCreationEmail(cardHolder.getEmail());

                return CardMapper.toDto(card, cardHolder);
        }

        private Card createNewCard(Account account, BigDecimal cardLimit) {
                Card card = new Card();
                card.setAccount(account);
                card.setCardNumber(generateCardNumber());
                card.setCvv(generateCVV());
                card.setCreationDate(LocalDate.now());
                card.setExpirationDate(LocalDate.now().plusYears(3));
                card.setStatus(CardStatus.ACTIVE);
                card.setCardLimit(cardLimit);
                return card;
        }

        private String generateCardNumber() {
                Random random = new Random();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 16; i++) {
                        sb.append(random.nextInt(10));
                }
                return sb.toString();
        }

        private String generateCVV() {
                Random random = new Random();
                return String.format("%03d", random.nextInt(1000));
        }

        private void sendCardCreationEmail(String email) {
                EmailRequestDto emailRequestDto = new EmailRequestDto();
                emailRequestDto.setCode("CARD_CREATED");
                emailRequestDto.setDestination(email);
                rabbitTemplate.convertAndSend("card-creation", emailRequestDto);
        }
}