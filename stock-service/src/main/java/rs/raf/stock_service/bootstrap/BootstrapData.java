package rs.raf.stock_service.bootstrap;


import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.CountryService;
import rs.raf.stock_service.service.ExchangeService;
import rs.raf.stock_service.service.HolidayService;
import rs.raf.stock_service.domain.entity.*;

import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.domain.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class BootstrapData implements CommandLineRunner {

    private final CountryService countryService;
    private final CountryRepository countryRepository;
    private final ExchangeService exchangeService;
    private final HolidayService holidayService;
    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final ListingDailyPriceInfoRepository dailyPriceInfoRepository;
    private final ExchangeRepository exchangeRepository;

    @Override
    public void run(String... args) {
        // ili pokretati ovako zajedno, ili samo u importExchanges staviti druge dve funkcije na pocetku,
        // nisam znao da li je bolje da su ovako nepovezane ili da ih povezujem pa sam ostavio ovako
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();

              // Kreiranje Exchange-a
        Exchange nasdaq = new Exchange("XNAS", "NASDAQ", "NAS", countryRepository.findByName("United States").get(), "USD", -5L, false);
        exchangeRepository.save(nasdaq);

        // Kreiranje Stock-a (Apple)
        Stock appleStock = new Stock();
        appleStock.setTicker("AAPL");
        appleStock.setName("Apple Inc.");
        appleStock.setExchange(nasdaq);
        appleStock.setPrice(new BigDecimal("150.50"));
        appleStock.setAsk(new BigDecimal("151.00"));
        appleStock.setLastRefresh(LocalDateTime.now());
        appleStock.setOutstandingShares(5000000L);
        appleStock.setDividendYield(new BigDecimal("0.005"));

        listingRepository.save(appleStock);

        // Kreiranje Futures Contract-a (nafta)
        FuturesContract oilFutures = new FuturesContract();
        oilFutures.setTicker("OIL2025");
        oilFutures.setName("Crude Oil Future");
        oilFutures.setExchange(nasdaq);
        oilFutures.setPrice(new BigDecimal("75.20"));
        oilFutures.setAsk(new BigDecimal("75.50"));
        oilFutures.setLastRefresh(LocalDateTime.now());
        oilFutures.setContractSize(1000);
        oilFutures.setContractUnit("Barrels");
        oilFutures.setSettlementDate(LocalDate.of(2025, 6, 15));

        listingRepository.save(oilFutures);

        // Kreiranje ListingDailyPriceInfo podataka za Apple
        ListingDailyPriceInfo appleDailyInfo = new ListingDailyPriceInfo();
        appleDailyInfo.setListing(appleStock);
        appleDailyInfo.setDate(LocalDate.now());
        appleDailyInfo.setPrice(new BigDecimal("150.50"));
        appleDailyInfo.setHigh(new BigDecimal("152.00"));
        appleDailyInfo.setLow(new BigDecimal("149.00"));  // Low umesto bid
        appleDailyInfo.setChange(new BigDecimal("2.50"));
        appleDailyInfo.setVolume(2000000L);
        dailyPriceInfoRepository.save(appleDailyInfo);

        // Kreiranje ListingDailyPriceInfo podataka za naftne futures
        ListingDailyPriceInfo oilDailyInfo = new ListingDailyPriceInfo();
        oilDailyInfo.setListing(oilFutures);
        oilDailyInfo.setDate(LocalDate.now());
        oilDailyInfo.setPrice(new BigDecimal("75.20"));
        oilDailyInfo.setHigh(new BigDecimal("76.00"));
        oilDailyInfo.setLow(new BigDecimal("74.50"));  // Low umesto bid
        oilDailyInfo.setChange(new BigDecimal("-0.80"));
        oilDailyInfo.setVolume(500000L);
        dailyPriceInfoRepository.save(oilDailyInfo);

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
