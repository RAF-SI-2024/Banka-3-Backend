package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.dto.ClientDto;
import rs.raf.user_service.dto.CreateClientDto;
import rs.raf.user_service.dto.ErrorMessageDto;
import rs.raf.user_service.dto.UpdateClientDto;
import rs.raf.user_service.exceptions.EmailAlreadyExistsException;
import rs.raf.user_service.exceptions.JmbgAlreadyExistsException;
import rs.raf.user_service.service.ClientService;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/clients")
@Tag(name = "Client Management", description = "API for managing clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

   /*
    Parametri za pretragu se prosledjuju kao query parametri ne kao request body
    Endpoint vraca json u sledecem formatu
            "id": 1,
            "firstName": "Jovan",
            "lastName": "Jovanovic",
            "email": "jovan.v@example.com",
            "address": "Cara Dusana 105",
            "phone": "0671152371",
            "gender": "M",
            "birthDate": "1990-01-24T23:00:00.000+00:00"
     */

    // GET endpoint sa opcionalnim filterima i sortiranje po prezimenu
    @PreAuthorize("hasAuthority('admin') or hasAuthority('employee')")
    @GetMapping
    @Operation(summary = "Get all clients with filtering and pagination")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Clients retrieved successfully")})
    public ResponseEntity<Page<ClientDto>> getAllClients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<ClientDto> clients = clientService.listClientsWithFilters(firstName, lastName, email, pageable);
        return ResponseEntity.ok(clients);
    }

    @PreAuthorize("hasAuthority('admin') or hasAuthority('employee')")
    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<?> getClientById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok().body(clientService.getClientById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAuthority('admin') or hasAuthority('employee')")
    @PostMapping
    @Operation(summary = "Add new client (password is set during activation)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> addClient(@Valid @RequestBody CreateClientDto createClientDto) {
        try {
            ClientDto clientDto = clientService.addClient(createClientDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(clientDto);
        } catch (EmailAlreadyExistsException | JmbgAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('admin') or hasAuthority('employee')")
    @PutMapping("/{id}")
    @Operation(summary = "Update client (only allowed fields)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> updateClient(@PathVariable Long id, @Valid @RequestBody UpdateClientDto updateClientDto) {
        try {
            ClientDto clientDto = clientService.updateClient(id, updateClientDto);
            return ResponseEntity.status(HttpStatus.OK).body(clientDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('admin') or hasAuthority('employee')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete client by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        try {
            clientService.deleteClient(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current employee", description = "Returns the currently authenticated employee's details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved employee details"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentEmployee() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok().body(clientService.findByEmail(email));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
