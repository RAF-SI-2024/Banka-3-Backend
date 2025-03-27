package rs.raf.stock_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.FuturesContractDto;
import rs.raf.stock_service.domain.dto.OptionDto;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
    private final FuturesService futuresService;
    private final OptionService optionService;
    private final FuturesRepository futuresContractRepository;
    private final OptionRepository optionRepository;

    @Override
    public void run(String... args) {
        // Import core data
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();

        // Create an Exchange for stock listings
        Exchange nasdaq = new Exchange("XNAS", "NASDAQ", "NAS", countryRepository.findByName("United States").get(), "USD", -5L, false);
        exchangeRepository.save(nasdaq);

        // Import stocks and forex pairs using postojeće metode
        importStocks(nasdaq);
        importForexPairs();
        addStock(nasdaq);
        addStockWithDailyInfo(nasdaq);
//        loadOrders();

        // Futures and Options data
        addFutures();
        addOptionsForStocks();
    }

    private void addFutures() {
        // Učitaj futures kontrakte iz CSV fajla preko FuturesService
        List<FuturesContractDto> futuresDtos = futuresService.getFuturesContracts();
        for (FuturesContractDto dto : futuresDtos) {
            FuturesContract fc = new FuturesContract();
            fc.setTicker(dto.getTicker());
            fc.setContractSize(dto.getContractSize());
            fc.setContractUnit(dto.getContractUnit());
            fc.setSettlementDate(dto.getSettlementDate());
            fc.setMaintenanceMargin(dto.getMaintenanceMargin());
            fc.setPrice(dto.getPrice());
            futuresContractRepository.save(fc);
            System.out.println("Imported futures contract: " + fc.getTicker());
        }
    }

    private void addOptionsForStocks() {
        // 💡 Rešenje protiv beskonačne petlje:
        if (optionRepository.count() > 0) {
            System.out.println("Options already exist in the database. Skipping generation.");
            return;
        }

        List<Stock> stocks = listingRepository.findAll().stream()
                .filter(listing -> listing instanceof Stock)
                .map(listing -> (Stock) listing)
                .collect(Collectors.toList());

        int index = 0;
        for (Stock stock : stocks) {
            if (index == 3) break;
            BigDecimal currentPrice = stock.getPrice();
            List<OptionDto> optionDtos = optionService.generateOptions(stock.getTicker(), currentPrice);

            for (OptionDto dto : optionDtos) {
                Option opt = new Option();
                opt.setUnderlyingStock(stock);
                opt.setOptionType(index++ % 2 == 0 ? OptionType.CALL : OptionType.PUT);
                opt.setStrikePrice(dto.getStrikePrice());
                opt.setContractSize(dto.getContractSize());
                opt.setSettlementDate(dto.getSettlementDate());
                opt.setMaintenanceMargin(dto.getMaintenanceMargin());
                opt.setUnderlyingStock(stock);
                opt.setImpliedVolatility(BigDecimal.valueOf(1));
                opt.setOpenInterest(new Random().nextInt(500) + 100);

                optionRepository.save(opt);
            }

            System.out.println("Generated and imported options for stock: " + stock.getTicker());
        }
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
                // Pretpostavljamo da forexService.getForexPair vraća DTO, ali u bootstrap-u koristimo podaci iz DTO-ja
                var forexData = forexService.getForexPair(baseCurrency, quoteCurrency);

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
            } catch (Exception e) {
                System.err.println("Error importing forex pair: " + pair[0] + "/" + pair[1] + " - " + e.getMessage());
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
