package rs.raf.stock_service.bootstrap;


import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.service.CountryService;
import rs.raf.stock_service.service.ExchangeService;
import rs.raf.stock_service.service.HolidayService;

@Component
@AllArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final CountryService countryService;
    private final ExchangeService exchangeService;
    private final HolidayService holidayService;

    @Override
    public void run(String... args) {
        // ili pokretati ovako zajedno, ili samo u importExchanges staviti druge dve funkcije na pocetku,
        // nisam znao da li je bolje da su ovako nepovezane ili da ih povezujem pa sam ostavio ovako
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();
    }
}
