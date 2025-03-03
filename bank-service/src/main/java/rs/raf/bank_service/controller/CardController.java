package rs.raf.bank_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.dto.CardDto;
import rs.raf.bank_service.service.CardService;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<CardDto>> getClientCards(@PathVariable Long clientId) {
        return ResponseEntity.ok(cardService.getClientCards(clientId));
    }

    @PostMapping("/{cardNumber}/block")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<CardDto> blockCard(
            @PathVariable String cardNumber,
            @RequestParam Long clientId) {
        return ResponseEntity.ok(cardService.blockCard(cardNumber, clientId));
    }

    @PostMapping("/{cardNumber}/unblock")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CardDto> unblockCard(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.unblockCard(cardNumber));
    }

    @PostMapping("/{cardNumber}/deactivate")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CardDto> deactivateCard(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.deactivateCard(cardNumber));
    }
}