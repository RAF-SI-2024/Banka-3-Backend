package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CardDtoNoOwner;
import rs.raf.bank_service.domain.dto.CardRequestDto;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.service.CardService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/account/{accountNumber}/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @GetMapping
    @Operation(summary = "Get Cards by Account", description = "Retrieves all cards associated with the specified account number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully")
    })
    public ResponseEntity<List<CardDto>> getCardsByAccount(
            @Parameter(description = "Account number for which cards are retrieved", in = ParameterIn.PATH, required = true, example = "222222222222222222")
            @PathVariable String accountNumber) {
        List<CardDto> cards = cardService.getCardsByAccount(accountNumber);
        return ResponseEntity.ok(cards);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/{cardNumber}/block")
    @Operation(summary = "Block Card", description = "Blocks the card identified by the provided card number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked successfully")
    })
    public ResponseEntity<Void> blockCard(
            @Parameter(description = "Card number to block", in = ParameterIn.PATH, required = true, example = "1234123412341234")
            @PathVariable String cardNumber) {
        cardService.changeCardStatus(cardNumber, CardStatus.BLOCKED);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/request")
    @Operation(summary = "Request a card.", description = "Requests a new card for a given account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A confirmation email has been sent. Please verify to receive your card.")
    })
    public ResponseEntity<String> requestCardForAccount(@RequestBody @Valid CreateCardDto createCardDto) {
        cardService.requestCardForAccount(createCardDto);
        return ResponseEntity.ok("A confirmation email has been sent. Please verify to receive your card.");
    }

    @PostMapping("/receive")
    @Operation(summary = "Verify the token and receive a card.", description = "Verify the token and receive a card if the entered token is right.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token verified and card created successfully.")
    })
    public ResponseEntity<CardDtoNoOwner> verifyAndReceiveCard(@RequestBody @Valid CardRequestDto cardRequestDto) {
        CardDtoNoOwner cardDto = cardService.recieveCardForAccount(cardRequestDto.getToken(), cardRequestDto.getCreateCardDto());
        return ResponseEntity.ok(cardDto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/create")
    @Operation(summary = "Create a card.", description = "Create a new card.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card created successfully")
    })
    public ResponseEntity<CardDtoNoOwner> createCard(@RequestBody @Valid CreateCardDto createCardDto) {
        CardDtoNoOwner cardDto = cardService.createCard(createCardDto);
        return ResponseEntity.ok(cardDto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/{cardNumber}/unblock")
    @Operation(summary = "Unblock Card", description = "Unblocks the card identified by the provided card number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card unblocked successfully")
    })
    public ResponseEntity<Void> unblockCard(
            @Parameter(description = "Card number to unblock", in = ParameterIn.PATH, required = true, example = "1234123412341234")
            @PathVariable String cardNumber) {
        cardService.changeCardStatus(cardNumber, CardStatus.ACTIVE);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/{cardNumber}/deactivate")
    @Operation(summary = "Deactivate Card", description = "Deactivates the card identified by the provided card number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card deactivated successfully")
    })
    public ResponseEntity<Void> deactivateCard(
            @Parameter(description = "Card number to deactivate", in = ParameterIn.PATH, required = true, example = "1234123412341234")
            @PathVariable String cardNumber) {
        cardService.changeCardStatus(cardNumber, CardStatus.DEACTIVATED);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/{cardNumber}/block-by-user")
    @Operation(summary = "Block Card by User", description = "Allows a user to block their own card.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked successfully")
    })
    public ResponseEntity<Void> blockCardByUser(
            @Parameter(description = "Card number to block", in = ParameterIn.PATH, required = true, example = "1234123412341234")
            @PathVariable String cardNumber,
            @RequestHeader("Authorization") String authHeader) {
        cardService.blockCardByUser(cardNumber, authHeader);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my-cards")
    @Operation(summary = "Get User's Cards", description = "Retrieves all cards belonging to the authenticated user across all their accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully")
    })
    public ResponseEntity<List<CardDto>> getUserCards(@RequestHeader("Authorization") String authHeader) {
        List<CardDto> cards = cardService.getUserCards(authHeader);
        return ResponseEntity.ok(cards);
    }
}
