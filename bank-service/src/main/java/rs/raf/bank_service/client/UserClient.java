package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.*;

import javax.validation.Valid;
import java.util.List;


/// Klasa koja sluzi za slanje HTTP poziva na userService
@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8080}", fallbackFactory = UserClientFallbackFactory.class, decode404 = true)
public interface UserClient {

    @GetMapping("/api/admin/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);

    @PostMapping("/api/auth/request-card")
    void requestCard(RequestCardDto requestCardDto);

    @PostMapping("/api/auth/check-token")
    void checkToken(CheckTokenDto checkTokenDto);

    @GetMapping("/api/company/{id}")
    CompanyDto getCompanyById(@PathVariable("id") Long id);

    @GetMapping("/api/authorized-personnel/company/{companyId}")
    List<AuthorizedPersonelDto> getAuthorizedPersonnelByCompany(@PathVariable("companyId") Long companyId);

    @PostMapping("/api/verification/request")
    void createVerificationRequest(@RequestBody CreateVerificationRequestDto request);

    @PostMapping("/api/auth/login/employee")
    LoginResponseDto employeeLogin(@RequestBody LoginRequestDto request);

    @PostMapping("/api/admin/clients")
    ClientDto addClient(@Valid @RequestBody CreateClientDto createClientDto);

    @PostMapping("/api/auth/set-password")
    void activateUser(@RequestBody ActivationRequestDto activationRequestDto);

    @GetMapping("/api/admin/clients")
    ResponseEntity<Page<ClientDto>> getAllClients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);

    @PostMapping("/api/auth/login/client")
    LoginResponseDto clientLogin(@RequestBody LoginRequestDto request);

    @GetMapping("/api/authorized-personnel/{id}")
    AuthorizedPersonelDto getAuthorizedPersonnelById(@PathVariable("id") Long id);

}


