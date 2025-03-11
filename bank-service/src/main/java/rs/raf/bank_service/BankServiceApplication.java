package rs.raf.bank_service;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import rs.raf.bank_service.service.ExchangeRateService;

@SpringBootApplication
@EnableFeignClients(basePackages = "rs.raf.bank_service.client")
@EnableScheduling
@AllArgsConstructor
public class BankServiceApplication implements CommandLineRunner {

    private final ExchangeRateService exchangeRateService;

    public static void main(String[] args) {
        SpringApplication.run(BankServiceApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // da ne trosimo api pozive
//        exchangeRateService.updateExchangeRates();
    }
}
