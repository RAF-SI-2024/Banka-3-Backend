package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.ListingDetailsDto;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.ListingFilterDto;
import rs.raf.stock_service.domain.dto.ListingUpdateDto;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.service.ListingService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    @Autowired
    private ListingService listingService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PreAuthorize("hasAnyRole('CLIENT', 'SUPERVISOR', 'AGENT', 'EMPLOYEE', 'ADMIN')")
    @GetMapping
    @Operation(summary = "Get filtered list of securities", description = "Returns a list of stocks, futures, or forex pairs based on filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Securities retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<ListingDto>> getListings(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String exchangePrefix,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minAsk,
            @RequestParam(required = false) BigDecimal maxAsk,
            @RequestParam(required = false) BigDecimal minBid,
            @RequestParam(required = false) BigDecimal maxBid,
            @RequestParam(required = false) Long minVolume,
            @RequestParam(required = false) Long maxVolume,
            @RequestParam(required = false) BigDecimal minMaintenanceMargin,
            @RequestParam(required = false) BigDecimal maxMaintenanceMargin,
            @RequestParam(required = false) LocalDate settlementDate,
            @RequestParam(required = false, defaultValue = "price") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder
    ) {
        String role = jwtTokenUtil.getUserRoleFromAuthHeader(token);

        ListingFilterDto filter = new ListingFilterDto();
        filter.setType(type);
        filter.setSearch(search);
        filter.setExchangePrefix(exchangePrefix);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setMinAsk(minAsk);
        filter.setMaxAsk(maxAsk);
        filter.setMinBid(minBid); // Sada predstavlja low
        filter.setMaxBid(maxBid); // Sada predstavlja low
        filter.setMinVolume(minVolume);
        filter.setMaxVolume(maxVolume);
        filter.setMinMaintenanceMargin(minMaintenanceMargin);
        filter.setMaxMaintenanceMargin(maxMaintenanceMargin);
        filter.setSettlementDate(settlementDate);
        filter.setSortBy(sortBy);
        filter.setSortOrder(sortOrder);

        return ResponseEntity.ok(listingService.getListings(filter, role));
    }
    @PreAuthorize("hasAnyRole('CLIENT', 'SUPERVISOR', 'AGENT', 'EMPLOYEE', 'ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "Get details of a security", description = "Returns detailed information about a specific stock, future, or forex pair.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Security details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Security not found")
    })
    public ResponseEntity<ListingDetailsDto> getListingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListingDetails(id));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update a security", description = "Allows a supervisor to update the price and ask values of a listing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listing updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ResponseEntity<ListingDto> updateListing(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody ListingUpdateDto updateDto
    ) {
        try {
            return ResponseEntity.ok(listingService.updateListing(id, updateDto, authHeader));
        } catch (ListingNotFoundException ex) {
            throw ex;
        }
    }
}
