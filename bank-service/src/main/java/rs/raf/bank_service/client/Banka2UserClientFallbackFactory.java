package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.web.bind.annotation.RequestParam;
import rs.raf.bank_service.domain.dto.Banka2AccountResponseDto;

import java.util.Optional;

public class Banka2UserClientFallbackFactory implements FallbackFactory<Banka2UserClient> {
    @Override
    public Banka2UserClient create(Throwable cause) {
        return new Banka2UserClient() {
            @Override
            public Optional<Banka2AccountResponseDto> getAccountByAccountNumber(@RequestParam("Number") String accountNumber) {
                return null;
            }
        };
    }
}
