package rs.raf.bank_service.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import rs.raf.bank_service.dto.ClientDto;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface ClientServiceClient {  // OVO MORA BITI INTERFEJS
    @GetMapping("/api/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);
}
