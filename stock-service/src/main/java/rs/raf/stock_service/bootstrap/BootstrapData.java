package rs.raf.stock_service.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.*;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;
import org.springframework.context.ApplicationContext;
import rs.raf.stock_service.client.AlphavantageClient;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
    @Autowired private CountryRepository countryRepository;
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
    @Autowired private OrderRepository orderRepository;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private AlphavantageClient alphavantageClient;
    @Autowired private OtcOptionRepository otcOptionRepository;
    @Autowired private OtcOfferRepository otcOfferRepository;
    @Autowired private ListingRedisService listingRedisService;
    @Autowired private ListingMapper listingMapper;

    @Value("${bootstrap.thread.pool.size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int threadPoolSize;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(String... args) {
        log.info("Starting db seeder");
        importCoreData();
        if (listingRepository.count() == 0) {
            importStocksAndHistory();
            importForexAndHistory();
            addFutures();
        }

        listingRedisService.clear();

        List<ListingDto> dtos = listingRepository.findAll().stream()
                .map(listing -> listingMapper.toDto(listing, priceHistoryRepository.findTopByListingOrderByDateDesc(listing)))
                .toList();
        listingRedisService.saveAll(dtos);
        log.info("Listings saved to redis");

//        addPortfolioTestData();
//        addOrderTestData();
//        addOtcOfferTestData();
//        addOtcOptionTestData();
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

    private void importStocks() {
        Map<String, List<String>> stockTickersByExchange = Map.of(
                "BATS", List.of("CGTL", "ZBZX", "CBOE", "ZTEST", "GBXA", "HIMU", "IYRI", "JANU", "KDEC"),
                "NASDAQ", List.of("AACBU", "BACK", "CAAS", "DADA", "EA", "FA", "GABC", "HAFC", "IAC", "JACK"),
                "NYSE", List.of("A", "BA", "C", "D", "E", "F", "G", "H", "IAG", "J"),
                "NYSE ARCA", List.of("AGRW", "ASLV", "BENJ", "ESBA", "CPXR", "FISK", "LGDX", "OGCP", "PCFI"),
                "NYSE MKT", List.of("ACCS", "BATL", "CANF", "DC", "EFSH", "FOXO", "GAU", "HCWC", "IAUX", "JOB")
        );

        List<Stock> allStocks = Collections.synchronizedList(new ArrayList<>());

        refreshInParallel(new ArrayList<>(stockTickersByExchange.entrySet()), entry -> {
            Exchange exchange = exchangeRepository.findByMic(entry.getKey());
            if (exchange == null) return;

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

                    allStocks.add(stock);
                } catch (StockNotFoundException e) {
                    log.warn("Stock not found: {}", ticker);
                }
            }
        });

        saveInBatches(allStocks, 500, listingRepository::saveAllAndFlush);
        log.info("Imported stocks");
    }

    private void importStockPriceHistory() {
        List<Stock> stocks = listingRepository.findAll().stream()
                .filter(s -> s instanceof Stock)
                .map(s -> (Stock) s)
                .toList();

        List<ListingPriceHistory> all = refreshInParallel(stocks, stock -> {
            try {
                TimeSeriesDto dto = listingService.getPriceHistoryFromAlphaVantage(stock.getTicker(), "5min", "compact");
                Set<LocalDateTime> existing = priceHistoryRepository.findDatesByListingId(stock.getId());
                return createNewHistory(stock, dto, existing);
            } catch (Exception e) {
                log.warn("Stock history failed for {}", stock.getTicker(), e);
                return List.of();
            }
        });

        saveInBatches(all, 100, priceHistoryRepository::saveAllAndFlush);
        log.info("Imported stock price history");

    }

    private void importForexPairs() {
        List<String[]> pairs = List.of(
                new String[]{"EUR", "USD"}, new String[]{"GBP", "USD"}, new String[]{"JPY", "USD"},
                new String[]{"CAD", "USD"}, new String[]{"RSD", "USD"}, new String[]{"NZD", "USD"},
                new String[]{"CHF", "USD"}
        );
        Exchange exchange = exchangeRepository.findByMic("NASDAQ");
        List<ForexPair> list = refreshInParallel(pairs, pair -> {
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
                fx.setExchange(exchange);
                return List.of(fx);
            } catch (Exception e) {
                return List.of();
            }
        });

        saveInBatches(list, 500, listingRepository::saveAllAndFlush);
        log.info("Imported forex pairs");

    }

    private void importForexPriceHistory() {
        List<ForexPair> pairs = listingRepository.findAll().stream()
                .filter(f -> f instanceof ForexPair)
                .map(f -> (ForexPair) f)
                .toList();

        List<ListingPriceHistory> all = refreshInParallel(pairs, pair -> {
            try {
                TimeSeriesDto dto = listingService.getForexPriceHistory(pair.getId(), "5min");
                Set<LocalDateTime> existing = priceHistoryRepository.findDatesByListingId(pair.getId());
                return createNewHistory(pair, dto, existing);
            } catch (Exception e) {
                log.warn("Forex history failed for {}", pair.getTicker(), e);
                return List.of();
            }
        });

        saveInBatches(all, 100, priceHistoryRepository::saveAllAndFlush);
        log.info("Imported forex price history");

    }

    @Transactional
    public void addFutures() {
        Exchange exchange = exchangeRepository.findByMic("NASDAQ");
        List<FuturesContractDto> dtos = futuresService.getFuturesContracts();
        List<FuturesContract> list = dtos.stream().map(dto -> {
            FuturesContract f = new FuturesContract();
            f.setTicker(dto.getTicker());
            f.setPrice(dto.getPrice());
            f.setContractSize(dto.getContractSize());
            f.setSettlementDate(dto.getSettlementDate());
            f.setMaintenanceMargin(dto.getMaintenanceMargin());
            f.setContractUnit(dto.getContractUnit());
            f.setExchange(exchange);
            return f;
        }).toList();

        saveInBatches(list, 200, futuresRepository::saveAllAndFlush);
        log.info("Imported futures");

    }

    @Transactional
    public void addOtcOfferTestData() {
        if (otcOfferRepository.count() > 0) return;
        Stock stock = (Stock) listingRepository.findByTicker("DADA").orElse(null);

        OtcOffer otcOffer1 = OtcOffer.builder()
                .id(1L)
                .premium(new BigDecimal("5.00"))
                .status(OtcOfferStatus.PENDING)
                .amount(10)
                .settlementDate(LocalDate.now().plusMonths(1))
                .sellerId(1L)
                .buyerId(2L)
                .pricePerStock(new BigDecimal("23"))
                .lastModified(LocalDateTime.now())
                .lastModifiedById(2L)
                .stock(stock)
                .build();

        OtcOffer otcOffer2 = OtcOffer.builder()
                .id(2L)
                .premium(new BigDecimal("15.00"))
                .status(OtcOfferStatus.PENDING)
                .amount(10)
                .settlementDate(LocalDate.now().plusMonths(1))
                .sellerId(1L)
                .buyerId(2L)
                .pricePerStock(new BigDecimal("146.56"))
                .lastModified(LocalDateTime.now())
                .lastModifiedById(1L)
                .stock(stock)
                .build();

        otcOfferRepository.save(otcOffer1);
        otcOfferRepository.save(otcOffer2);
    }

    @Transactional
    public void addOtcOptionTestData() {
        if (otcOptionRepository.count() > 0) return;
        Stock stock = (Stock) listingRepository.findByTicker("DADA").orElse(null);

        // Validni neiskorišćeni ugovor
        OtcOption option1 = OtcOption.builder()
                .strikePrice(new BigDecimal("2.00"))
                .settlementDate(LocalDate.now().plusMonths(3))
                .amount(100)
                .buyerId(1L)
                .sellerId(2L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.VALID)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        // Istekao iskorišćen ugovor
        OtcOption option2 = OtcOption.builder()
                .strikePrice(new BigDecimal("2.00"))
                .settlementDate(LocalDate.now().minusDays(15))
                .amount(50)
                .buyerId(1L)
                .sellerId(2L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.USED)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        // Validni ugovor
        OtcOption option3 = OtcOption.builder()
                .strikePrice(new BigDecimal("1.00"))
                .settlementDate(LocalDate.now().plusWeeks(2))
                .amount(200)
                .buyerId(2L)
                .sellerId(1L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.VALID)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

         // Validni ugovor
         OtcOption option4 = OtcOption.builder()
                 .strikePrice(new BigDecimal("2.00"))
                 .settlementDate(LocalDate.now().plusWeeks(2))
                 .amount(200)
                 .buyerId(2L)
                 .sellerId(1L)
                 .underlyingStock(stock)
                 .status(OtcOptionStatus.VALID)
                 .premium(new BigDecimal("50.00"))
                 .otcOffer(OtcOffer.builder()
                         .premium(new BigDecimal("50.00"))
                         .status(OtcOfferStatus.ACCEPTED)
                         .build())
                 .build();

        // Validni ugovor
        OtcOption option5 = OtcOption.builder()
                .strikePrice(new BigDecimal("3.00"))
                .settlementDate(LocalDate.now().plusWeeks(2))
                .amount(200)
                .buyerId(2L)
                .sellerId(1L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.VALID)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        // Validni ugovor
        OtcOption option6 = OtcOption.builder()
                .strikePrice(new BigDecimal("4.00"))
                .settlementDate(LocalDate.now().plusWeeks(2))
                .amount(200)
                .buyerId(2L)
                .sellerId(1L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.VALID)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        // Validni ugovor
        OtcOption option7 = OtcOption.builder()
                .strikePrice(new BigDecimal("5.00"))
                .settlementDate(LocalDate.now().plusWeeks(2))
                .amount(200)
                .buyerId(2L)
                .sellerId(1L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.VALID)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        // Istekao neiskorišćen ugovor
        OtcOption option8 = OtcOption.builder()
                .strikePrice(new BigDecimal("2.00"))
                .settlementDate(LocalDate.now().minusMonths(1))
                .amount(75)
                .buyerId(1L)
                .sellerId(2L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.VALID)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        OtcOption option9 = OtcOption.builder()
                .strikePrice(new BigDecimal("2.00"))
                .settlementDate(LocalDate.now().plusMonths(3))
                .amount(100)
                .buyerId(1L)
                .sellerId(2L)
                .underlyingStock(stock)
                .status(OtcOptionStatus.USED)
                .premium(new BigDecimal("50.00"))
                .otcOffer(OtcOffer.builder()
                        .premium(new BigDecimal("50.00"))
                        .status(OtcOfferStatus.ACCEPTED)
                        .build())
                .build();

        List<OtcOption> options = List.of(option1, option2, option3, option4, option5, option6, option7, option8, option9);

        // Postavi bidirectional vezu za otcOffer
        options.forEach(option -> {
            if(option.getOtcOffer() != null) {
                option.getOtcOffer().setOtcOption(option);
            }
        });

        otcOptionRepository.saveAll(options);
    }

    @Transactional
    public void addPortfolioTestData() {
        if (portfolioEntryRepository.count() == 0) return;

        PortfolioEntry p1 = PortfolioEntry.builder()
                .id(1L).amount(1000).type(ListingType.STOCK).used(false)
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

    @Transactional
    public void addOrderTestData() {
        if (orderRepository.count() > 0) return;
        Listing stock = listingRepository.findByTicker("DADA").orElse(null);

        Order user1DoneBuy = Order.builder()
                .id(1L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("211111111111111111")
                .afterHours(false)
                .isDone(true)
                .direction(OrderDirection.BUY)
                .pricePerUnit(new BigDecimal("50"))
                .remainingPortions(0)
                .taxAmount(BigDecimal.ZERO)
                .taxStatus(TaxStatus.TAXFREE)
                .userId(1L)
                .userRole("CLIENT")
                .quantity(10)
                .approvedBy(null)
                .status(OrderStatus.DONE)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(new BigDecimal(500))
                .commission(new BigDecimal(7))
                .build();

        Order user2Pending = Order.builder()
                .id(2L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("111111111111111111")
                .afterHours(false)
                .isDone(false)
                .direction(OrderDirection.BUY)
                .pricePerUnit(new BigDecimal("140"))
                .remainingPortions(20)
                .taxAmount(BigDecimal.ZERO)
                .taxStatus(TaxStatus.TAXFREE)
                .userId(5L)
                .userRole("AGENT")
                .quantity(20)
                .approvedBy(null)
                .status(OrderStatus.PENDING)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(BigDecimal.ZERO)
                .build();

        Order user1DoneSell = Order.builder()
                .id(3L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("211111111111111111")
                .afterHours(false)
                .isDone(true)
                .direction(OrderDirection.SELL)
                .pricePerUnit(new BigDecimal("60"))
                .remainingPortions(0)
                .taxAmount(new BigDecimal("15"))
                .taxStatus(TaxStatus.PENDING)
                .userId(1L)
                .userRole("CLIENT")
                .quantity(10)
                .approvedBy(null)
                .status(OrderStatus.DONE)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(new BigDecimal(600))
                .commission(new BigDecimal(7))
                .profit(new BigDecimal("100"))
                .build();

        Order user1DoneBuy2 = Order.builder()
                .id(4L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("211111111111111111")
                .afterHours(false)
                .isDone(true)
                .direction(OrderDirection.BUY)
                .pricePerUnit(new BigDecimal("50"))
                .remainingPortions(0)
                .taxAmount(BigDecimal.ZERO)
                .taxStatus(TaxStatus.TAXFREE)
                .userId(1L)
                .userRole("CLIENT")
                .quantity(10)
                .approvedBy(null)
                .status(OrderStatus.DONE)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(new BigDecimal(500))
                .commission(new BigDecimal(7))
                .build();

        Order user1DoneSell2 = Order.builder()
                .id(5L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("211111111111111111")
                .afterHours(false)
                .isDone(true)
                .direction(OrderDirection.SELL)
                .pricePerUnit(new BigDecimal("55"))
                .remainingPortions(0)
                .taxAmount(new BigDecimal("7.5"))
                .taxStatus(TaxStatus.PAID)
                .userId(1L)
                .userRole("CLIENT")
                .quantity(10)
                .approvedBy(null)
                .status(OrderStatus.DONE)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(new BigDecimal(550))
                .commission(new BigDecimal(7))
                .profit(new BigDecimal("50"))
                .build();

        Order user3DoneBuy = Order.builder()
                .id(6L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("333000157555885522")
                .afterHours(false)
                .isDone(true)
                .direction(OrderDirection.BUY)
                .pricePerUnit(new BigDecimal("10"))
                .remainingPortions(0)
                .taxAmount(BigDecimal.ZERO)
                .taxStatus(TaxStatus.TAXFREE)
                .userId(3L)
                .userRole("ADMIN")
                .quantity(100)
                .approvedBy(null)
                .status(OrderStatus.DONE)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(new BigDecimal(1000))
                .commission(new BigDecimal(0))
                .build();

        Order user3DoneSell = Order.builder()
                .id(7L)
                .orderType(OrderType.MARKET)
                .contractSize(1)
                .accountNumber("333000157555885522")
                .afterHours(false)
                .isDone(true)
                .direction(OrderDirection.SELL)
                .pricePerUnit(new BigDecimal("12"))
                .remainingPortions(0)
                .taxAmount(new BigDecimal("30"))
                .taxStatus(TaxStatus.PAID)
                .userId(3L)
                .userRole("ADMIN")
                .quantity(100)
                .approvedBy(null)
                .status(OrderStatus.DONE)
                .lastModification(LocalDateTime.now())
                .listing(stock)
                .totalPrice(new BigDecimal(3000))
                .commission(new BigDecimal(0))
                .profit(new BigDecimal("200"))
                .build();


        orderRepository.saveAll(List.of(
                user1DoneBuy,
                user2Pending,
                user1DoneBuy,
                user1DoneSell,
                user3DoneBuy,
                user3DoneSell,
                user1DoneBuy2,
                user1DoneSell2,
                user3DoneBuy,
                user3DoneSell
        ));
    }

    private List<ListingPriceHistory> createNewHistory(Listing listing, TimeSeriesDto dto, Set<LocalDateTime> existingDates) {
        return dto.getValues().stream()
                .map(v -> {
                    LocalDateTime date = LocalDateTime.parse(v.getDatetime(), formatter);
                    if (existingDates.contains(date)) return null;
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

    private <T> void refreshInParallel(List<T> items, Consumer<T> task) {
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        for (T item : items) {
            executor.submit(() -> {
                try {
                    task.accept(item);
                } catch (Exception e) {
                    log.error("Parallel task error", e);
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("Parallel task timeout", e);
        }
    }

    private <T, R> List<R> refreshInParallel(List<T> items, Function<T, List<R>> task) {
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<List<R>>> futures = new ArrayList<>();

        for (T item : items) {
            futures.add(executor.submit(() -> task.apply(item)));
        }

        List<R> result = new ArrayList<>();
        for (Future<List<R>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (Exception e) {
                log.error("Parallel fetch failed", e);
            }
        }

        executor.shutdown();
        return result;
    }

    private BootstrapData getSelfProxy() {
        return applicationContext.getBean(BootstrapData.class);
    }
}
