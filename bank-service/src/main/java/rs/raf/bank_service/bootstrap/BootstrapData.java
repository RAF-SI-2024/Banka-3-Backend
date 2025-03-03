package rs.raf.bank_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.repository.CurrencyRepository;

@AllArgsConstructor
@Component
public class BootstrapData implements CommandLineRunner {
    private final CurrencyRepository currencyRepository;

    @Override
    public void run(String... args) throws Exception {
        if (currencyRepository.count() == 0) {
            Currency currencyEuro = Currency.builder()
                    .code("EUR")
                    .name("Euro")
                    .symbol("€")
                    .countries("Montenegro,Greece,Germany")
                    .description("Euro neuro")
                    .active(true)
                    .build();


            currencyRepository.save(currencyEuro);
        }

    }
}
