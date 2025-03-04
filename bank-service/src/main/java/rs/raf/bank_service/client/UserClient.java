package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.CompanyDto;

/// Klasa koja sluzi za slanje HTTP poziva na userService
@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8080}")
public interface UserClient {

    @GetMapping("/api/admin/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);

    @GetMapping("/api/admin/clients/me")
    ClientDto getClient(@RequestHeader("Authorization") String authorizationHeader);

    @GetMapping("/api/admin/company/{id}")
    CompanyDto getCompanyById(@RequestHeader("Authorization") String authorizationHeader, @PathVariable("id") Long id);
}