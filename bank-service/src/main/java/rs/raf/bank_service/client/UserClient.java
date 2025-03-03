package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import rs.raf.bank_service.domain.dto.ClientDto;

/// Klasa koja sluzi za slanje HTTP poziva na userService
@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8080}")
public interface UserClient {

    @GetMapping("/api/admin/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);
}