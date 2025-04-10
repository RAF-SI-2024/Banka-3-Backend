package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.exceptions.OptionNotEligibleException;
import rs.raf.stock_service.service.PortfolioService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final JwtTokenUtil jwtTokenUtil;

    @Operation(
            summary = "Get portfolio for authenticated user",
            description = "Returns a list of securities the user owns. Accessible to CLIENT, AGENT and SUPERVISOR roles."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio retrieved successfully."),
            @ApiResponse(responseCode = "204", description = "Portfolio is empty."),
            @ApiResponse(responseCode = "403", description = "Access denied – only CLIENT, AGENT and SUPERVISOR roles allowed."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPERVISOR')")
    @GetMapping
    public ResponseEntity<?> getPortfolio(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            List<PortfolioEntryDto> portfolio = portfolioService.getPortfolioForUser(userId);

            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Set public amount for a specific stock in user's portfolio",
            description = "Allows CLIENT, AGENT, and SUPERVISOR roles to set the number of shares marked as public for a specific listing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public amount updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input or user doesn't own the specified listing."),
            @ApiResponse(responseCode = "403", description = "Access denied – only CLIENT, AGENT and SUPERVISOR roles allowed."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    @PutMapping("/public-amount")
    public ResponseEntity<?> setPublicAmount(@RequestHeader("Authorization") String authHeader,
                                             @RequestBody SetPublicAmountDto dto) {
        try {
            Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            portfolioService.setPublicAmount(userId, dto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get all public stocks (OTC portal)",
            description = "Returns all STOCK type securities from all users that are marked as public (publicAmount > 0). Accessible to CLIENT, AGENT, SUPERVISOR and ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public stocks retrieved successfully."),
            @ApiResponse(responseCode = "204", description = "No public stocks available."),
            @ApiResponse(responseCode = "403", description = "Access denied – only CLIENT, AGENT, SUPERVISOR and ADMIN roles allowed."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'SUPERVISOR', 'ADMIN')")
    @GetMapping("/public-stocks")
    public ResponseEntity<?> getAllPublicStocks() {
        try {
            List<PublicStockDto> result = portfolioService.getAllPublicStocks();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('AGENT') or hasRole('CLIENT')")
    @GetMapping("/tax")
    @Operation(summary = "Get user taxes", description = "Returns paid tax for current year, and unpaid for current month.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Taxes obtained successfully"),
    })
    public ResponseEntity<TaxGetResponseDto>getUserTaxes(@RequestHeader("Authorization") String authHeader){
        Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok().body(portfolioService.getUserTaxes(userId));

    }

    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT')")
    @PostMapping("/use-option")
    @Operation(summary = "Use option (CALL/PUT) if eligible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Option used successfully"),
            @ApiResponse(responseCode = "400", description = "Option not eligible or invalid request"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<?> useOption(@RequestHeader("Authorization") String authHeader,
                                       @RequestBody UseOptionDto dto) {
        try {
            Long userId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
            portfolioService.useOption(userId, dto);
            return ResponseEntity.ok().build();
        } catch (OptionNotEligibleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
