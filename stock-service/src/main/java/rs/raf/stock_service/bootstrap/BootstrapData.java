package rs.raf.stock_service.bootstrap;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class BootstrapData implements CommandLineRunner {

    private final CountryService countryService;
    private final CountryRepository countryRepository;
    private final ExchangeService exchangeService;
    private final HolidayService holidayService;
    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final ListingService listingService;
    private final ListingPriceHistoryRepository dailyPriceInfoRepository;
    private final ExchangeRepository exchangeRepository;
    private final StocksService stocksService;
    private final ForexService forexService;
    private final FuturesService futuresService;
    private final OptionService optionService;
    private final FuturesRepository futuresContractRepository;
    private final OptionRepository optionRepository;
    private final AlphavantageClient alphavantageClient;
    private final ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        // Pozivanje self-proxy metoda kako bi se osigurale transakcije
        getSelfProxy().importCoreData();
        getSelfProxy().importStocksAndPriceHistory();
        getSelfProxy().importForexPairsAndPriceHistory();
        getSelfProxy().addFutures();
        getSelfProxy().addOptionsForStocks();
    }

    @Transactional
    public void importCoreData() {
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();
        System.out.println("Core data successfully imported.");
    }

    @Transactional
    public void importStocksAndPriceHistory() {
        importStocks();
        importStockPriceHistory();
    }

    @Transactional
    public void importForexPairsAndPriceHistory() {
        importForexPairs();
        importForexPriceHistory();
    }

    @Transactional
    public void addFutures() {
        List<FuturesContractDto> futuresDtos = futuresService.getFuturesContracts();
        List<FuturesContract> futuresContracts = futuresDtos.stream()
                .map(dto -> {
                    FuturesContract fc = new FuturesContract();
                    fc.setTicker(dto.getTicker());
                    fc.setContractSize(dto.getContractSize());
                    fc.setContractUnit(dto.getContractUnit());
                    fc.setSettlementDate(dto.getSettlementDate());
                    fc.setMaintenanceMargin(dto.getMaintenanceMargin());
                    fc.setPrice(dto.getPrice());
                    return fc;
                }).collect(Collectors.toList());

        futuresContractRepository.saveAll(futuresContracts);
        System.out.println("Futures contracts imported successfully.");
    }

    @Transactional
    public void addOptionsForStocks() {
        if (optionRepository.count() > 0) {
            System.out.println("Options already exist. Skipping generation.");
            return;
        }

        List<Stock> stocks = listingRepository.findAll().stream()
                .filter(listing -> listing instanceof Stock)
                .map(listing -> (Stock) listing)
                .collect(Collectors.toList());

        List<Option> optionsToSave = new ArrayList<>();
        for (Stock stock : stocks) {
            List<OptionDto> optionDtos = optionService.generateOptions(stock.getTicker(), stock.getPrice());
            for (OptionDto dto : optionDtos) {
                Option opt = new Option();
                opt.setUnderlyingStock(stock);
                opt.setOptionType(dto.getOptionType());
                opt.setStrikePrice(dto.getStrikePrice());
                opt.setContractSize(dto.getContractSize());
                opt.setSettlementDate(dto.getSettlementDate());
                opt.setMaintenanceMargin(dto.getMaintenanceMargin());
                opt.setImpliedVolatility(BigDecimal.valueOf(1));
                opt.setOpenInterest(new Random().nextInt(500) + 100);
                opt.setPrice(dto.getPrice());
                opt.setTicker(dto.getTicker());
                optionsToSave.add(opt);
            }
        }

        optionRepository.saveAll(optionsToSave);
        System.out.println("Options successfully imported.");
    }

    private void importStocks() {
        System.out.println("Importing selected stocks...");
        Map<String, List<String>> stockTickersByExchange = Map.of(
                "BATS", List.of("CGTL", "ZBZX", "CBOE", "ZTEST", "ZTST", "GBXA", "HIMU", "IYRI", "JANU", "KDEC"),
                "NASDAQ", List.of("AACBU", "BACK", "CAAS", "DADA", "EA", "FA", "GABC", "HAFC", "IAC", "JACK"),
                "NYSE", List.of("A", "BA", "C", "D", "E", "F", "G", "H", "IAG", "J"),
                "NYSE ARCA", List.of("AGRW", "ASLV", "BENJ", "ESBA", "CPXR", "FISK", "IGZ", "LGDX", "OGCP", "PCFI"),
                "NYSE MKT", List.of("ACCS", "BATL", "CANF", "DC", "EFSH", "FOXO", "GAU", "HCWC", "IAUX", "JOB")
        );

        List<Stock> allStocks = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : stockTickersByExchange.entrySet()) {
            String exchangeName = entry.getKey();
            List<String> stockTickers = entry.getValue();

            Exchange exchange = exchangeRepository.findByMic(exchangeName);
            if (exchange == null) {
                System.err.println("Exchange not found: " + exchangeName);
                continue;
            }

            for (String ticker : stockTickers) {
                try {
                    StockDto stockData = stocksService.getStockData(ticker);
                    if (stockData == null){
                        continue;
                    }
                    Stock stock = new Stock();
                    stock.setTicker(stockData.getTicker());
                    stock.setName(stockData.getName());
                    stock.setPrice(stockData.getPrice());
                    stock.setChange(stockData.getChange());
                    stock.setMaintenanceMargin(stockData.getMaintenanceMargin());
                    stock.setMarketCap(stockData.getMarketCap());
                    stock.setDividendYield(stockData.getDividendYield());
                    stock.setVolume(stockData.getVolume());
                    stock.setOutstandingShares(stockData.getOutstandingShares());
                    stock.setExchange(exchange);

                    allStocks.add(stock);
                } catch (StockNotFoundException e) {
                    System.err.println("Stock not found for: " + ticker);
                }
            }
        }

        listingRepository.saveAll(allStocks);
        System.out.println("Stocks imported successfully.");
    }

    private void importStocksBulk() {
        System.out.println("Importing stocks using Realtime Bulk Quotes...");

        Map<String, List<String>> stockTickersByExchange = Map.of(
                "BATS", List.of("CGTL", "ZBZX", "CBOE", "ZTEST", "ZTST", "GBXA", "HIMU", "IYRI", "JANU", "KDEC"),
                "NASDAQ", List.of("AACBU", "BACK", "CAAS", "DADA", "EA", "FA", "GABC", "HAFC", "IAC", "JACK"),
                "NYSE", List.of("A", "BA", "C", "D", "E", "F", "G", "H", "IAG", "J"),
                "NYSE ARCA", List.of("AGRW", "ASLV", "BENJ", "ESBA", "CPXR", "FISK", "IGZ", "LGDX", "OGCP", "PCFI"),
                "NYSE MKT", List.of("ACCS", "BATL", "CANF", "DC", "EFSH", "FOXO", "GAU", "HCWC", "IAUX", "JOB")
        );

        List<Stock> allStocks = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : stockTickersByExchange.entrySet()) {
            String exchangeName = entry.getKey();
            List<String> tickers = entry.getValue();

            Exchange exchange = exchangeRepository.findByMic(exchangeName);
            if (exchange == null) {
                System.err.println("Exchange not found: " + exchangeName);
                continue;
            }

            List<StockDto> bulkStocksData = stocksService.getRealtimeBulkStockData(tickers);

            for (StockDto stockDto : bulkStocksData) {
                Stock stock = new Stock();
                stock.setTicker(stockDto.getTicker());
                stock.setPrice(stockDto.getPrice());
                stock.setVolume(stockDto.getVolume());
                stock.setExchange(exchange);
                stock.setName(stockDto.getName());
                allStocks.add(stock);
            }
        }

        listingRepository.saveAll(allStocks);
        System.out.println("Bulk stocks imported successfully.");
    }


    private void importStockPriceHistory() {
        List<Stock> stocks = listingRepository.findAll().stream()
                .filter(listing -> listing instanceof Stock)
                .map(listing -> (Stock) listing)
                .collect(Collectors.toList());

        List<ListingPriceHistory> priceHistoryEntities = new ArrayList<>();
        for (Stock stock : stocks) {
            try {
                TimeSeriesDto priceHistory = listingService.getPriceHistoryFromAlphaVantage(stock.getTicker(), "5min", "compact");
                priceHistoryEntities.addAll(createPriceHistoryEntities(stock, priceHistory));
            } catch (Exception e) {
                System.err.println("Error fetching price history for stock: " + stock.getTicker());
            }
        }

        dailyPriceInfoRepository.saveAll(priceHistoryEntities);
        System.out.println("Stock price history imported successfully.");
    }

    private void importForexPairs() {
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

        List<ForexPair> allForexPairs = new ArrayList<>();
        for (String[] pair : forexPairs) {
            try {
                String baseCurrency = pair[0];
                String quoteCurrency = pair[1];
                ForexPairDto forexData = forexService.getForexPair(baseCurrency, quoteCurrency);

                ForexPair forexPair = new ForexPair();
                forexPair.setName(forexData.getName());
                forexPair.setTicker(forexData.getTicker());
                forexPair.setBaseCurrency(forexData.getBaseCurrency());
                forexPair.setQuoteCurrency(forexData.getQuoteCurrency());
                forexPair.setExchangeRate(forexData.getExchangeRate());
                forexPair.setMaintenanceMargin(forexData.getMaintenanceMargin());
                forexPair.setLiquidity(forexData.getLiquidity());
                forexPair.setLastRefresh(forexData.getLastRefresh());
                forexPair.setNominalValue(forexData.getNominalValue());
                forexPair.setAsk(forexData.getAsk());
                forexPair.setPrice(forexData.getPrice());

                allForexPairs.add(forexPair);
            } catch (Exception e) {
                System.err.println("Error importing forex pair: " + pair[0] + "/" + pair[1]);
            }
        }

        listingRepository.saveAll(allForexPairs);
        System.out.println("Forex pairs imported successfully.");
    }

    private void importForexPriceHistory() {
        List<ForexPair> forexPairs = listingRepository.findAll().stream()
                .filter(listing -> listing instanceof ForexPair)
                .map(listing -> (ForexPair) listing)
                .collect(Collectors.toList());

        List<ListingPriceHistory> priceHistoryEntities = new ArrayList<>();
        for (ForexPair forex : forexPairs) {
            try {
                TimeSeriesDto priceHistory = listingService.getForexPriceHistory(forex.getId(), "5min");
                priceHistoryEntities.addAll(createPriceHistoryEntities(forex, priceHistory));
            } catch (Exception e) {
                System.err.println("Error fetching price history for forex pair: " + forex.getTicker());
            }
        }

        dailyPriceInfoRepository.saveAll(priceHistoryEntities);
        System.out.println("Forex price history imported successfully.");
    }

    private List<ListingPriceHistory> createPriceHistoryEntities(Listing listing, TimeSeriesDto priceHistory) {
        List<ListingPriceHistory> priceHistoryEntities = new ArrayList<>();
        for (TimeSeriesDto.TimeSeriesValueDto value : priceHistory.getValues()) {
            ListingPriceHistory priceHistoryEntity = ListingPriceHistory.builder()
                    .listing(listing)
                    .date(LocalDateTime.parse(value.getDatetime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .open(value.getOpen())
                    .high(value.getHigh())
                    .low(value.getLow())
                    .close(value.getClose())
                    .volume(value.getVolume())
                    .change(value.getClose().subtract(value.getOpen()))
                    .build();
            priceHistoryEntities.add(priceHistoryEntity);
        }
        return priceHistoryEntities;
    }


    private BootstrapData getSelfProxy() {
        return applicationContext.getBean(BootstrapData.class);
    }
}
