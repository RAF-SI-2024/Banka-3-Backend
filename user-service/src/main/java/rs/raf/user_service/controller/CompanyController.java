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
import rs.raf.user_service.dto.CreateCompanyDto;
import rs.raf.user_service.service.CompanyService;
import rs.raf.user_service.service.EmployeeService;

import javax.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/company")
@Tag(name = "Company Management", description = "API for managing companies")
@AllArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    //@PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Create new company", description = "Creates new company with provided parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created company"),
            @ApiResponse(responseCode = "400", description = "Invalid data.")
    })
    @PostMapping
    public ResponseEntity<String> createCompany(@RequestBody CreateCompanyDto createCompanyDto) {
        try {
            companyService.createCompany(createCompanyDto);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }
}
