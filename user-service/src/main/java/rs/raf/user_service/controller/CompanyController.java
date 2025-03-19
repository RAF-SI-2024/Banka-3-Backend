package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.domain.dto.CompanyDto;
import rs.raf.user_service.domain.dto.CreateCompanyDto;
import rs.raf.user_service.domain.dto.ErrorMessageDto;
import rs.raf.user_service.exceptions.*;
import rs.raf.user_service.service.CompanyService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/company")
@Tag(name = "Company Management", description = "API for managing companies")
@AllArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Create new company", description = "Creates new company with provided parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created company"),
            @ApiResponse(responseCode = "400", description = "Invalid data.")
    })
    @PostMapping
    public ResponseEntity<?> createCompany(@RequestBody @Valid CreateCompanyDto createCompanyDto) {
        try {
            CompanyDto companyDto = companyService.createCompany(createCompanyDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(companyDto);
        } catch (ClientNotFoundException | ActivityCodeNotFoundException | CompanyRegNumExistsException |
                TaxIdAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(e.getMessage()));
        }

    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get company", description = "Retrieves company by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved company"),
            @ApiResponse(responseCode = "404", description = "Company not found."),
            @ApiResponse(responseCode = "500", description = "Company retrieval failed.")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok().body(companyService.getCompanyById(id));
        } catch (CompanyNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @Operation(summary = "Get companies", description = "Retrieves companies by client id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved companies"),
            @ApiResponse(responseCode = "500", description = "Company retrieval failed.")
    })
    @GetMapping("/owned-by/{id}")
    public ResponseEntity<?> getCompaniesForClientId(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok().body(companyService.getCompaniesForClientId(id));
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body(new ErrorMessageDto(e.getMessage()));
        }
    }
}
