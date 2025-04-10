package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import rs.raf.stock_service.domain.dto.ActuaryDto;
import rs.raf.stock_service.domain.dto.ActuaryLimitDto;
import rs.raf.stock_service.domain.dto.ClientDto;
import rs.raf.stock_service.domain.dto.UserTaxDto;

import java.util.List;


/// Klasa koja sluzi za slanje HTTP poziva na userService
@FeignClient(name = "user-service", url = "${spring.cloud.openfeign.client.config.user-service.url}", decode404 = true)
public interface UserClient {

    @GetMapping("/api/admin/actuaries/{id}")
    ActuaryLimitDto getActuaryByEmployeeId(@PathVariable("id") Long id);

    @GetMapping("/api/admin/clients/{id}")
    ClientDto getClientById(@PathVariable("id") Long id);

    @GetMapping("/api/admin/employees/{id}")
    ActuaryDto getEmployeeById(@PathVariable("id") Long id);

    @GetMapping("/api/admin/actuaries/all")
    List<UserTaxDto> getAgentsAndClients(@RequestParam String name, @RequestParam String surname,@RequestParam String role);


}


