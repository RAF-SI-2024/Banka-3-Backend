package rs.raf.stock_service.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BootstrapData implements CommandLineRunner {

    @Autowired private CountryService countryService;
    @Autowired private ExchangeService exchangeService;
    @Autowired private HolidayService holidayService;
    @Autowired private ListingRepository listingRepository;
    @Autowired private ListingPriceHistoryRepository priceHistoryRepository;
    @Autowired private ExchangeRepository exchangeRepository;
    @Autowired private StocksService stocksService;
    @Autowired private ForexService forexService;
    @Autowired private FuturesService futuresService;
    @Autowired private OptionService optionService;
    @Autowired private FuturesRepository futuresRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private PortfolioEntryRepository portfolioEntryRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private ListingService listingService;

    @Value("${bootstrap.thread.pool.size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int threadPoolSize;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(String... args) {
        importCoreData();
        importStocksAndHistory();
        importForexAndHistory();
        addFutures();
        addOptions();
        addPortfolioTestData();
    }

    private void importCoreData() {
        countryService.importCountries();
        holidayService.importHolidays();
        exchangeService.importExchanges();
    }

    private void importStocksAndHistory() {
        importStocks();
        importStockPriceHistory();
    }

    private void importForexAndHistory() {
        importForexPairs();
        importForexPriceHistory();
    }

    @Transactional
    public void addPortfolioTestData() {
        PortfolioEntry p1 = PortfolioEntry.builder()
                .id(1L).amount(100).type(ListingType.STOCK).used(false)
                .averagePrice(new BigDecimal("154")).userId(1L)
                .inTheMoney(false)
                .listing(listingRepository.findByTicker("DADA").orElseThrow())
                .publicAmount(50).lastModified(LocalDateTime.now()).build();

        PortfolioEntry p2 = PortfolioEntry.builder()
                .id(2L).amount(59).type(ListingType.STOCK).used(false)
                .averagePrice(new BigDecimal("442")).userId(2L)
                .inTheMoney(false)
                .listing(listingRepository.findByTicker("BACK").orElseThrow())
                .publicAmount(30).lastModified(LocalDateTime.now()).build();

        portfolioEntryRepository.saveAllAndFlush(List.of(p1, p2));
    }

    private void importStocks() {
        Map<String, List<String>> stockTickersByExchange = Map.of(
                "BATS", List.of("CGTL", "ZBZX", "CBOE", "ZTEST", "ZTST", "GBXA", "HIMU", "IYRI", "JANU", "KDEC"),
                "NASDAQ", List.of("AACBU", "BACK", "CAAS", "DADA", "EA", "FA", "GABC", "HAFC", "IAC", "JACK"),
                "NYSE", List.of("A", "BA", "C", "D", "E", "F", "G", "H", "IAG", "J"),
                "NYSE ARCA", List.of("AGRW", "ASLV", "BENJ", "ESBA", "CPXR", "FISK", "IGZ", "LGDX", "OGCP", "PCFI"),
                "NYSE MKT", List.of("ACCS", "BATL", "CANF", "DC", "EFSH", "FOXO", "GAU", "HCWC", "IAUX", "JOB")
        );

        List<Stock> stocks = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : stockTickersByExchange.entrySet()) {
            Exchange exchange = exchangeRepository.findByMic(entry.getKey());
            if (exchange == null) continue;

            for (String ticker : entry.getValue()) {
                try {
                    StockDto dto = stocksService.getStockData(ticker);
                    if (dto == null) continue;
                    Stock stock = new Stock();
                    stock.setTicker(dto.getTicker());
                    stock.setName(dto.getName());
                    stock.setPrice(dto.getPrice());
                    stock.setChange(dto.getChange());
                    stock.setMaintenanceMargin(dto.getMaintenanceMargin());
                    stock.setMarketCap(dto.getMarketCap());
                    stock.setDividendYield(dto.getDividendYield());
                    stock.setVolume(dto.getVolume());
                    stock.setOutstandingShares(dto.getOutstandingShares());
                    stock.setExchange(exchange);
                    stocks.add(stock);
                } catch (StockNotFoundException e) {
                    log.warn("Stock not found: {}", ticker);
                }
            }
        }

        saveInBatches(stocks, 500, listingRepository::saveAllAndFlush);
        System.out.println("Zavrsio stock import");
    }

    private void importStockPriceHistory() {
        List<Stock> stocks = listingRepository.findAll().stream().filter(s -> s instanceof Stock).map(s -> (Stock) s).toList();

        List<ListingPriceHistory> all = parallelFetch(stocks, threadPoolSize, stock -> {
            try {
                TimeSeriesDto dto = listingService.getPriceHistoryFromAlphaVantage(stock.getTicker(), "5min", "compact");
                Set<LocalDateTime> existing = priceHistoryRepository.findDatesByListingId(stock.getId());
                return createNewHistory(stock, dto, existing);
            } catch (Exception e) {
                return List.of();
            }
        });

        saveInBatches(all, 100, priceHistoryRepository::saveAllAndFlush);
        System.out.println("Zavrsio stock history import");
    }

    private void importForexPairs() {
        List<String[]> pairs = List.of(
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
        List<ForexPair> list = new ArrayList<>();
        for (String[] pair : pairs) {
            try {
                ForexPairDto dto = forexService.getForexPair(pair[0], pair[1]);
                ForexPair fx = new ForexPair();
                fx.setName(dto.getName());
                fx.setTicker(dto.getTicker());
                fx.setBaseCurrency(dto.getBaseCurrency());
                fx.setQuoteCurrency(dto.getQuoteCurrency());
                fx.setExchangeRate(dto.getExchangeRate());
                fx.setMaintenanceMargin(dto.getMaintenanceMargin());
                fx.setLiquidity(dto.getLiquidity());
                fx.setLastRefresh(dto.getLastRefresh());
                fx.setNominalValue(dto.getNominalValue());
                fx.setAsk(dto.getAsk());
                fx.setPrice(dto.getPrice());
                list.add(fx);
            } catch (Exception ignored) {}
        }

        saveInBatches(list, 500, listingRepository::saveAllAndFlush);
        System.out.println("Zavrsio forex import");
    }

    private void importForexPriceHistory() {
        List<ForexPair> pairs = listingRepository.findAll().stream().filter(f -> f instanceof ForexPair).map(f -> (ForexPair) f).toList();

        List<ListingPriceHistory> all = parallelFetch(pairs, threadPoolSize, pair -> {
            try {
                TimeSeriesDto dto = listingService.getForexPriceHistory(pair.getId(), "5min");
                Set<LocalDateTime> existing = priceHistoryRepository.findDatesByListingId(pair.getId());
                return createNewHistory(pair, dto, existing);
            } catch (Exception e) {
                return List.of();
            }
        });

        saveInBatches(all, 100, priceHistoryRepository::saveAllAndFlush);
        System.out.println("Zavrsio forex history import");
    }

    @Transactional
    public void addFutures() {
        List<FuturesContractDto> dtos = futuresService.getFuturesContracts();
        List<FuturesContract> list = dtos.stream().map(dto -> {
            FuturesContract f = new FuturesContract();
            f.setTicker(dto.getTicker());
            f.setPrice(dto.getPrice());
            f.setContractSize(dto.getContractSize());
            f.setSettlementDate(dto.getSettlementDate());
            f.setMaintenanceMargin(dto.getMaintenanceMargin());
            f.setContractUnit(dto.getContractUnit());
            return f;
        }).toList();

        saveInBatches(list, 200, futuresRepository::saveAllAndFlush);
        System.out.println("Zavrsio futures import");
    }

    @Transactional
    public void addOptions() {
        if (optionRepository.count() > 0) return;

        List<Stock> stocks = listingRepository.findAll().stream().filter(s -> s instanceof Stock).map(s -> (Stock) s).toList();

        List<Option> all = parallelFetch(stocks, threadPoolSize, stock -> {
            try {
                List<OptionDto> dtos = optionService.generateOptions(stock.getTicker(), stock.getPrice());
                return dtos.stream().map(dto -> {
                    Option o = new Option();
                    o.setUnderlyingStock(stock);
                    o.setOptionType(dto.getOptionType());
                    o.setStrikePrice(dto.getStrikePrice());
                    o.setContractSize(dto.getContractSize());
                    o.setSettlementDate(dto.getSettlementDate());
                    o.setMaintenanceMargin(dto.getMaintenanceMargin());
                    o.setPrice(dto.getPrice());
                    o.setTicker(dto.getTicker());
                    o.setImpliedVolatility(BigDecimal.ONE);
                    o.setOpenInterest(new Random().nextInt(500) + 100);
                    o.setOnSale(true);
                    return o;
                }).toList();
            } catch (Exception e) {
                return List.of();
            }
        });

        saveInBatches(all, 100, optionRepository::saveAllAndFlush);
        System.out.println("Zavrsio options import");
    }

    private List<ListingPriceHistory> createNewHistory(Listing listing, TimeSeriesDto dto, Set<LocalDateTime> existing) {
        return dto.getValues().stream()
                .map(v -> {
                    LocalDateTime date = LocalDateTime.parse(v.getDatetime(), formatter);
                    if (existing.contains(date)) return null;
                    return ListingPriceHistory.builder()
                            .listing(listing)
                            .date(date)
                            .open(v.getOpen())
                            .high(v.getHigh())
                            .low(v.getLow())
                            .close(v.getClose())
                            .volume(v.getVolume())
                            .change(v.getClose().subtract(v.getOpen()))
                            .build();
                }).filter(Objects::nonNull).toList();
    }

    private <T> void saveInBatches(List<T> list, int size, Consumer<List<T>> saver) {
        for (int i = 0; i < list.size(); i += size) {
            int end = Math.min(i + size, list.size());
            saver.accept(list.subList(i, end));
            entityManager.clear();
        }
    }

    private <T, R> List<R> parallelFetch(List<T> list, int threads, Function<T, List<R>> fn) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<List<R>>> futures = list.stream().map(item ->
                pool.submit(() -> fn.apply(item))).toList();

        List<R> results = new ArrayList<>();
        for (Future<List<R>> future : futures) {
            try {
                results.addAll(future.get());
            } catch (Exception ignored) {}
        }

        pool.shutdown();
        return results;
    }
}
