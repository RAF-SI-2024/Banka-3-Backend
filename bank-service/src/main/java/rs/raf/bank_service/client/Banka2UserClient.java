package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rs.raf.bank_service.domain.dto.Banka2AccountResponseDto;

import java.util.Optional;

@FeignClient(name = "banka2-userservice", url = "${spring.cloud.openfeign.client.config.banka2-userservice.url}", fallbackFactory = Banka2UserClientFallbackFactory.class)
public interface Banka2UserClient {

    @GetMapping("/api/v1/accounts")
    Optional<Banka2AccountResponseDto> getAccountByAccountNumber(@RequestParam("Number") String accountNumber);
}
