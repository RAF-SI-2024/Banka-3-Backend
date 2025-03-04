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
import rs.raf.bank_service.domain.dto.CreatePersonalCardDto;
import rs.raf.bank_service.domain.dto.CreateCompanyCardDto;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.service.CardService;

import java.util.List;

@RestController
@RequestMapping("/api/account/{accountNumber}/cards")
public class CardController {

        private final CardService cardService;

        public CardController(CardService cardService) {
                this.cardService = cardService;
        }

        @PreAuthorize("hasAuthority('admin')")
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

        @PreAuthorize("hasAuthority('admin')")
        @PostMapping("/{cardNumber}/block")
        @Operation(summary = "Block Card", description = "Blocks the card identified by the provided card number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> blockCard(
                        @Parameter(description = "Card number to block", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber) {
                cardService.changeCardStatus(cardNumber, CardStatus.BLOCKED);
                return ResponseEntity.ok().build();
        }

        @PreAuthorize("hasAuthority('admin')")
        @PostMapping("/{cardNumber}/unblock")
        @Operation(summary = "Unblock Card", description = "Unblocks the card identified by the provided card number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card unblocked successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> unblockCard(
                        @Parameter(description = "Card number to unblock", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber) {
                cardService.changeCardStatus(cardNumber, CardStatus.ACTIVE);
                return ResponseEntity.ok().build();
        }

        @PreAuthorize("hasAuthority('admin')")
        @PostMapping("/{cardNumber}/deactivate")
        @Operation(summary = "Deactivate Card", description = "Deactivates the card identified by the provided card number.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card deactivated successfully"),
                        @ApiResponse(responseCode = "404", description = "Card not found"),
                        @ApiResponse(responseCode = "403", description = "Access denied")
        })
        public ResponseEntity<Void> deactivateCard(
                        @Parameter(description = "Card number to deactivate", in = ParameterIn.PATH, required = true, example = "1234123412341234") @PathVariable String cardNumber) {
                cardService.changeCardStatus(cardNumber, CardStatus.DEACTIVATED);
                return ResponseEntity.ok().build();
        }

        @PreAuthorize("hasAuthority('client')")
        @PostMapping("create/personal")
        @Operation(summary = "Create Personal Card", description = "Creates a new card for a personal account. Only the account owner can create cards.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card created successfully"),
                        @ApiResponse(responseCode = "403", description = "Not authorized to create card for this account"),
                        @ApiResponse(responseCode = "404", description = "Account not found")
        })
        public ResponseEntity<CardDto> createPersonalCard(
                        @RequestHeader("Authorization") String authHeader,
                        @RequestBody CreatePersonalCardDto createPersonalCardDto) {
                CardDto card = cardService.createPersonalCard(createPersonalCardDto, authHeader);
                return ResponseEntity.ok(card);
        }

        @PreAuthorize("hasAuthority('client')")
        @PostMapping("create/company")
        @Operation(summary = "Create Company Card", description = "Creates a new card for a company account. Only the majority owner can create cards for authorized personnel.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Card created successfully"),
                        @ApiResponse(responseCode = "403", description = "Not authorized to create card for this company"),
                        @ApiResponse(responseCode = "404", description = "Account or authorized personnel not found")
        })
        public ResponseEntity<CardDto> createCompanyCard(
                        @RequestHeader("Authorization") String authHeader,
                        @RequestBody CreateCompanyCardDto createCompanyCardDto) {
                CardDto card = cardService.createCompanyCard(createCompanyCardDto, authHeader);
                return ResponseEntity.ok(card);
        }

        @PreAuthorize("hasAuthority('client')")
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
                cardService.blockCardByUser(cardNumber, authHeader);
                return ResponseEntity.ok().build();
        }

        @PreAuthorize("hasAuthority('client')")
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