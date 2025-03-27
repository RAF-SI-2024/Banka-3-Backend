package rs.raf.stock_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ForexPairDto;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.exceptions.ForexPairNotFoundException;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final StocksService stocksService;
    private final ForexService forexService;

    @Override
    public void run(String... args) {
        // Import core data
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();

        // Create an Exchange for stock listings
        Exchange nasdaq = new Exchange("XNAS", "NASDAQ", "NAS", countryRepository.findByName("United States").get(), "USD", -5L, false);
        exchangeRepository.save(nasdaq);

        // Import full Stocks data (LIMITED TO 10 API CALLS)
        importStocks(nasdaq);

        // Import full Forex Pairs data (LIMITED TO 10 API CALLS)
        importForexPairs();

        addStock(nasdaq);
        addFutures(nasdaq);
        addOption(nasdaq);
        addForex();
        addStockWithDailyInfo(nasdaq);
    }
    private void addStockWithDailyInfo(Exchange exchange) {
        Stock stock = new Stock();
        stock.setTicker("FILTERTEST");
        stock.setName("Filter Test Stock");
        stock.setPrice(new BigDecimal("123.45"));
        stock.setAsk(new BigDecimal("125.00"));
        stock.setDividendYield(new BigDecimal("1.23"));
        stock.setMarketCap(new BigDecimal("100000000"));
        stock.setOutstandingShares(1_000_000L);
        stock.setVolume(50000L);
        stock.setChange(new BigDecimal("2.15"));
        stock.setExchange(exchange);
        stock.setMaintenanceMargin(new BigDecimal("12.34"));

        listingRepository.save(stock); // mora prvo da se sačuva da bi imao ID

        // Dodaj ListingDailyPriceInfo (da bi testirao low, volume itd.)
        ListingDailyPriceInfo info = new ListingDailyPriceInfo();
        info.setListing(stock); // povezivanje
        info.setDate(LocalDate.now());
        info.setPrice(stock.getPrice());
        info.setLow(new BigDecimal("120.00")); // <-- FILTERI ĆE OVO TESTIRATI
        info.setHigh(new BigDecimal("130.00"));
        info.setChange(stock.getChange());
        info.setVolume(stock.getVolume());

        dailyPriceInfoRepository.save(info);

    }
    private void addStock(Exchange exchange) {
        Stock stock = new Stock();
        stock.setTicker("TEST1");
        stock.setName("Test Stock");
        stock.setPrice(new BigDecimal("100.00"));
        stock.setAsk(new BigDecimal("101.00"));
        stock.setDividendYield(new BigDecimal("2.5"));
        stock.setMarketCap(new BigDecimal("500000000"));
        stock.setOutstandingShares(1000000L);
        stock.setVolume(60000L);
        stock.setChange(new BigDecimal("1.5"));
        stock.setExchange(exchange);
        stock.setMaintenanceMargin(new BigDecimal("10.00"));

        listingRepository.save(stock);


        // Dodaj ListingDailyPriceInfo (da bi testirao low, volume itd.)
        ListingDailyPriceInfo info = new ListingDailyPriceInfo();
        info.setListing(stock); // povezivanje
        info.setDate(LocalDate.now());
        info.setPrice(stock.getPrice());
        info.setLow(new BigDecimal("123.00")); // <-- FILTERI ĆE OVO TESTIRATI
        info.setHigh(new BigDecimal("130.00"));
        info.setChange(stock.getChange());
        info.setVolume(stock.getVolume());

        dailyPriceInfoRepository.save(info);
    }

    private void addFutures(Exchange exchange) {
        FuturesContract futures = new FuturesContract();
        futures.setTicker("FUT1");
        futures.setName("Test Futures");
        futures.setPrice(new BigDecimal("1500.00"));
        futures.setAsk(new BigDecimal("1510.00"));
        futures.setContractSize(10);
        futures.setSettlementDate(LocalDate.now().plusMonths(1));
        futures.setExchange(exchange);

        listingRepository.save(futures);
    }

    private void addOption(Exchange exchange) {
        Option option = new Option();
        option.setTicker("OPT1");
        option.setName("Test Option");
        option.setPrice(new BigDecimal("5.00"));
        option.setAsk(new BigDecimal("5.50"));
        option.setOptionType(option.getOptionType().CALL);
        option.setStrikePrice(new BigDecimal("120.00"));
        option.setSettlementDate(LocalDate.now().plusWeeks(2));
        option.setImpliedVolatility(new BigDecimal("0.25"));
        option.setExchange(exchange);

        listingRepository.save(option);
    }

    private void addForex() {
        ForexPair pair = new ForexPair();
        pair.setTicker("USD/EUR");
        pair.setName("USD to EUR");
        pair.setPrice(new BigDecimal("0.92"));
        pair.setAsk(new BigDecimal("0.93"));
        pair.setBaseCurrency("USD");
        pair.setQuoteCurrency("EUR");
        pair.setExchangeRate(new BigDecimal("0.925"));
        pair.setLiquidity("HIGH");
        pair.setLastRefresh(LocalDateTime.now());
        pair.setNominalValue(new BigDecimal("100000"));
        pair.setMaintenanceMargin(new BigDecimal("10.00"));

        listingRepository.save(pair);
    }

    private void importStocks(Exchange exchange) {
        System.out.println("Importing selected stocks...");

        List<String> stockTickers = List.of("AAPL", "MSFT", "GOOGL", "IBM", "TSM", "MA", "NVDA", "META", "DIS", "BABA");

        for (String ticker : stockTickers) {
            try {
                // Fetch full details for selected stocks
                StockDto stockData = stocksService.getStockData(ticker);

                Stock stock = new Stock();
                stock.setTicker(stockData.getTicker());
                stock.setName(stockData.getName());
                stock.setPrice(stockData.getPrice());
                stock.setChange(stockData.getChange());
                stock.setVolume(stockData.getVolume());
                stock.setOutstandingShares(stockData.getOutstandingShares());
                stock.setDividendYield(stockData.getDividendYield());
                stock.setMarketCap(stockData.getMarketCap());
                stock.setMaintenanceMargin(stockData.getMaintenanceMargin());
                stock.setExchange(exchange); // Assign NASDAQ exchange

                listingRepository.save(stock);
                System.out.println("Imported stock: " + stock.getTicker());
            } catch (StockNotFoundException e) {
                System.err.println("Stock data not found for: " + ticker + " - " + e.getMessage());
            }
        }

        System.out.println("Finished importing selected stocks.");
    }

    private void importForexPairs() {
        System.out.println("Importing selected forex pairs...");

        List<String[]> forexPairs = List.of(
                new String[]{"USD", "EUR"},
                new String[]{"USD", "GBP"},
                new String[]{"USD", "JPY"},
                new String[]{"USD", "CAD"},
                new String[]{"USD", "AUD"},
                new String[]{"EUR", "GBP"},
                new String[]{"EUR", "JPY"},
                new String[]{"EUR", "CHF"},
                new String[]{"GBP", "JPY"},
                new String[]{"AUD", "NZD"}
        );

        for (String[] pair : forexPairs) {
            try {
                String baseCurrency = pair[0];
                String quoteCurrency = pair[1];

                // Fetch full details for selected forex pairs
                ForexPairDto forexData = forexService.getForexPair(baseCurrency, quoteCurrency);

                ForexPair forexPair = new ForexPair();
                forexPair.setName(forexData.getName());
                forexPair.setTicker(forexData.getTicker());
                forexPair.setBaseCurrency(forexData.getBaseCurrency());
                forexPair.setQuoteCurrency(forexData.getQuoteCurrency());
                forexPair.setExchangeRate(forexData.getExchangeRate());
                forexPair.setLiquidity(forexData.getLiquidity());
                forexPair.setLastRefresh(forexData.getLastRefresh());
                forexPair.setMaintenanceMargin(forexData.getMaintenanceMargin());
                forexPair.setNominalValue(forexData.getNominalValue());
                forexPair.setAsk(forexData.getAsk());
                forexPair.setPrice(forexData.getPrice());

                listingRepository.save(forexPair);
                System.out.println("Imported forex pair: " + forexPair.getTicker());
            } catch (ForexPairNotFoundException e) {
                System.err.println("Forex pair data not found for: " + pair[0] + "/" + pair[1] + " - " + e.getMessage());
            }
        }

        System.out.println("Finished importing selected forex pairs.");
    }

    //Smatram da order ne treba da se kreira, jer onda ne mozemo da pokrenemo execution, barem meni nema logike
//    private void loadOrders() {
//        if (orderRepository.count() == 0) {
//            Order order1 = Order.builder()
//                    .userId(1L)
//                    .listing(100L)
//                    .orderType(OrderType.LIMIT)
//                    .quantity(10)
//                    .contractSize(1)
//                    .pricePerUnit(new BigDecimal("150.50"))
//                    .direction(OrderDirection.BUY)
//                    .status(OrderStatus.PENDING)
//                    .approvedBy(null)
//                    .isDone(false)
//                    .lastModification(LocalDateTime.now())
//                    .remainingPortions(10)
//                    .afterHours(false)
//                    .build();
//
//            Order order2 = Order.builder()
//                    .userId(2L)
//                    .listing(200L)
//                    .orderType(OrderType.MARKET)
//                    .quantity(5)
//                    .contractSize(2)
//                    .pricePerUnit(new BigDecimal("200.00"))
//                    .direction(OrderDirection.SELL)
//                    .status(OrderStatus.APPROVED)
//                    .approvedBy(1L)
//                    .isDone(true)
//                    .lastModification(LocalDateTime.now().minusDays(1))
//                    .remainingPortions(0)
//                    .afterHours(true)
//                    .build();
//
//            orderRepository.saveAll(List.of(order1, order2));
//
//            System.out.println("Loaded initial test orders into database.");
//        }
//    }
}
