package rs.raf.stock_service.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.CreateOtcOfferDto;
import rs.raf.stock_service.domain.dto.OtcOfferDto;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.service.OtcService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/otc")
@RequiredArgsConstructor
public class OtcOfferController {

    private final OtcService otcService;
    private final JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "Create OTC offer", description = "Allows CLIENT or AGENT to create an OTC offer for a public stock.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "OTC offer created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input or insufficient shares."),
            @ApiResponse(responseCode = "403", description = "Access denied."),
            @ApiResponse(responseCode = "404", description = "Stock or seller not found."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    @PostMapping
    public ResponseEntity<?> createOtcOffer(@RequestHeader("Authorization") String authHeader,
                                            @Valid @RequestBody CreateOtcOfferDto dto) {
        try {
            Long buyerId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            OtcOfferDto created = otcService.createOffer(dto, buyerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all active OTC offers received by the user", description = "Returns pending offers where the user is the seller.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Offers retrieved."),
            @ApiResponse(responseCode = "204", description = "No active offers."),
            @ApiResponse(responseCode = "403", description = "Access denied."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    @GetMapping("/received")
    public ResponseEntity<?> getReceivedOffers(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            List<OtcOfferDto> offers = otcService.getAllActiveOffersForSeller(userId);
            return ResponseEntity.ok(offers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    public ResponseEntity<?> acceptOffer(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable Long id) {
        try {
            Long sellerId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            otcService.acceptOffer(id, sellerId);
            return ResponseEntity.ok("Offer successfully accepted.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Offer not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    public ResponseEntity<?> rejectOffer(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable Long id) {
        try {
            Long sellerId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            otcService.rejectOffer(id, sellerId);
            return ResponseEntity.ok("Offer successfully rejected.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Offer not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/counter")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    public ResponseEntity<?> counterOffer(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable Long id,
                                          @Valid @RequestBody CreateOtcOfferDto dto) {
        try {
            Long sellerId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            otcService.updateOffer(id, sellerId, dto);
            return ResponseEntity.ok("Counter-offer sent.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Offer not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }
}
