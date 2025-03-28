package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.PortfolioEntryDto;
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
}
