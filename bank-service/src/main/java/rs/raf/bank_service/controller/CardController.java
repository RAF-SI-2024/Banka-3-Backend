package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CardDtoNoOwner;
import rs.raf.bank_service.domain.dto.CardRequestDto;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.service.CardService;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/account/{accountNumber}/cards")
public class CardController {

        private final CardService cardService;

        public CardController(CardService cardService) {
                this.cardService = cardService;
        }

        @PreAuthorize("hasAuthority('employee')")
        @GetMapping
        @Operation(summary = "Get Cards by Account", description = "Retrieves all cards associated with the specified account number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<List<CardDto>> getCardsByAccount(
                        @Parameter(description = "Account number for which cards are retrieved", in = ParameterIn.PATH, required = true, example = "222222222222222222") @PathVariable String accountNumber) {
                List<CardDto> cards = cardService.getCardsByAccount(accountNumber);
                return ResponseEntity.ok(cards);
        }

        @PreAuthorize("hasAuthority('employee')")
        @PostMapping("/{cardNumber}/block")
        @Operation(summary = "Block Card", description = "Blocks the card identified by the provided card number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> blockCard(
                        @Parameter(description = "Card number to block", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber) {
                try {
                        cardService.changeCardStatus(cardNumber, CardStatus.BLOCKED);
                        return ResponseEntity.ok().build();
                } catch (EntityNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }

        }

    @PostMapping("/request")
    @Operation(
            summary = "Request a card.",
            description = "Requests a new card for a given account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A confirmation email has been sent. Please verify to receive your card."),
            @ApiResponse(responseCode = "404", description = "Account not found."),
            @ApiResponse(responseCode = "502", description = "Error in the communication of microservices."),
            @ApiResponse(responseCode = "400", description = "Invalid arguments.")
    })
    public ResponseEntity<String> requestCardForAccount(@RequestBody @Valid CreateCardDto createCardDto) {
        try {
            cardService.requestCardForAccount(createCardDto);
        } catch (EntityNotFoundException | ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (CardLimitExceededException | IllegalArgumentException | InvalidCardLimitException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getMessage());
        }
        return ResponseEntity.ok("A confirmation email has been sent. Please verify to receive your card.");
    }

    @PostMapping("/recieve")
    @Operation(
            summary = "Verify the token and recieve a card.",
            description = "Verify the token and recieve a card if the entered token is right."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token verified and card created successfully."),
            @ApiResponse(responseCode = "404", description = "Invalid token."),
            @ApiResponse(responseCode = "502", description = "Error in the communication of microservices."),
            @ApiResponse(responseCode = "400", description = "Invalid arguments.")
    })
    public ResponseEntity<CardDtoNoOwner> verifyAndReceiveCard(@RequestBody @Valid CardRequestDto cardRequestDto) {
        CardDtoNoOwner cardDto;
        try {
            cardDto = cardService.recieveCardForAccount(cardRequestDto.getToken(), cardRequestDto.getCreateCardDto());
        } catch (InvalidTokenException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (CardLimitExceededException | InvalidCardTypeException | InvalidCardLimitException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        return ResponseEntity.ok(cardDto);
    }

    @PostMapping("/create")
    @Operation(
            summary = "Create a card.",
            description = "Create a new card."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card created successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid token."),
            @ApiResponse(responseCode = "400", description = "Invalid arguments.")
    })
    public ResponseEntity<CardDtoNoOwner> createCard(@RequestBody @Valid CreateCardDto createCardDto) {
        CardDtoNoOwner cardDto;
        try {
            cardDto = cardService.createCard(createCardDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (CardLimitExceededException | InvalidCardLimitException | InvalidCardTypeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(cardDto);
    }

        @PreAuthorize("hasAuthority('employee')")
        @PostMapping("/{cardNumber}/unblock")
        @Operation(summary = "Unblock Card", description = "Unblocks the card identified by the provided card number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card unblocked successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> unblockCard(
                        @Parameter(description = "Card number to unblock", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber) {
                try {
                        cardService.changeCardStatus(cardNumber, CardStatus.ACTIVE);
                        return ResponseEntity.ok().build();
                }
                catch (EntityNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }
        }

        @PreAuthorize("hasAuthority('employee')")
        @PostMapping("/{cardNumber}/deactivate")
        @Operation(summary = "Deactivate Card", description = "Deactivates the card identified by the provided card number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card deactivated successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> deactivateCard(
                        @Parameter(description = "Card number to deactivate", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber) {
                try {
                        cardService.changeCardStatus(cardNumber, CardStatus.DEACTIVATED);
                        return ResponseEntity.ok().build();
                } catch (EntityNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }

        }

        @PreAuthorize("isAuthenticated()")
        @PostMapping("/{cardNumber}/block-by-user")
        @Operation(summary = "Block Card by User", description = "Allows a user to block their own card.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Not authorized to block this card")
        })
        public ResponseEntity<Void> blockCardByUser(
                        @Parameter(description = "Card number to block", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber,
                        @RequestHeader("Authorization") String authHeader) {
                try {
                        cardService.blockCardByUser(cardNumber, authHeader);
                        return ResponseEntity.ok().build();
                } catch (EntityNotFoundException e) {
                        return ResponseEntity.notFound().build();
                }

        }

        @PreAuthorize("isAuthenticated()")
        @GetMapping("/my-cards")
        @Operation(summary = "Get User's Cards", description = "Retrieves all cards belonging to the authenticated user across all their accounts.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<List<CardDto>> getUserCards(@RequestHeader("Authorization") String authHeader) {
                List<CardDto> cards = cardService.getUserCards(authHeader);
                return ResponseEntity.ok(cards);
        }

}