package rs.raf.stock_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ForexPairDto;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.domain.entity.ForexPair;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.enums.OrderType;
import rs.raf.stock_service.exceptions.ForexPairNotFoundException;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;

import java.math.BigDecimal;
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

        // Load Orders
        loadOrders();
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

    private void loadOrders() {
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
