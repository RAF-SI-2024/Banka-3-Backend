package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.service.InstallmentService;

import java.util.List;

@Tag(name = "Installments Controller", description = "API for managing loan installments")
@RestController
@RequestMapping("/api/installments")
public class InstallmentController {
    private final InstallmentService installmentService;

    public InstallmentController(InstallmentService installmentService) {
        this.installmentService = installmentService;
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Get installments by loan ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Installments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<InstallmentDto>> getInstallmentsByLoanId(@PathVariable Long loanId) {
        return ResponseEntity.ok(installmentService.getInstallmentsByLoanId(loanId));
    }


}

