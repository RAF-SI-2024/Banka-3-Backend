package rs.raf.stock_service.bootstrap;


import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.service.CountryService;
import rs.raf.stock_service.service.ExchangeService;
import rs.raf.stock_service.service.HolidayService;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.domain.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final CountryService countryService;
    private final ExchangeService exchangeService;
    private final HolidayService holidayService;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) {
        // ili pokretati ovako zajedno, ili samo u importExchanges staviti druge dve funkcije na pocetku,
        // nisam znao da li je bolje da su ovako nepovezane ili da ih povezujem pa sam ostavio ovako
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();

        if (orderRepository.count() == 0) {

            Order order1 = Order.builder()
                    .userId(1L)
                    .asset(100)
                    .orderType(OrderType.LIMIT)
                    .quantity(10)
                    .contractSize(1)
                    .pricePerUnit(new BigDecimal("150.50"))
                    .direction(OrderDirection.BUY)
                    .status(OrderStatus.PENDING)
                    .approvedBy(null)
                    .isDone(false)
                    .lastModification(LocalDateTime.now())
                    .remainingPortions(10)
                    .afterHours(false)
                    .build();

            Order order2 = Order.builder()
                    .userId(2L)
                    .asset(200)
                    .orderType(OrderType.MARKET)
                    .quantity(5)
                    .contractSize(2)
                    .pricePerUnit(new BigDecimal("200.00"))
                    .direction(OrderDirection.SELL)
                    .status(OrderStatus.APPROVED)
                    .approvedBy(1L)
                    .isDone(true)
                    .lastModification(LocalDateTime.now().minusDays(1))
                    .remainingPortions(0)
                    .afterHours(true)
                    .build();

            orderRepository.saveAll(List.of(order1, order2));

            System.out.println("Loaded initial test orders into database.");
        }
    }
}
