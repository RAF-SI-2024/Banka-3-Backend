package rs.raf.stock_service.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BootstrapData implements CommandLineRunner {

    private final ListingRepository listingRepository;
    private final ListingDailyPriceInfoRepository dailyPriceInfoRepository;
    private final ExchangeRepository exchangeRepository;

    public BootstrapData(ListingRepository listingRepository, ListingDailyPriceInfoRepository dailyPriceInfoRepository, ExchangeRepository exchangeRepository) {
        this.listingRepository = listingRepository;
        this.dailyPriceInfoRepository = dailyPriceInfoRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @Override
    public void run(String... args) {
        // Kreiranje Exchange-a
        Exchange nasdaq = new Exchange("XNAS", "NASDAQ", "NAS", "USA", "USD", -5L);
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
    }
}
