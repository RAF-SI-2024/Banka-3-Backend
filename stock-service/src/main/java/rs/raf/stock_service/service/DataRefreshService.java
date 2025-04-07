package rs.raf.stock_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.repository.*;

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
public class DataRefreshService {

    @Autowired private ListingRepository listingRepository;
    @Autowired private ListingPriceHistoryRepository priceHistoryRepository;
    @Autowired private PortfolioEntryRepository portfolioEntryRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private OptionService optionService;
    @Autowired private StocksService stocksService;
    @Autowired private ForexService forexService;
    @Autowired private ListingService listingService;
    @Autowired private EntityManager entityManager;

    @Value("${refresh.thread.pool.size:10}")
    private int threadPoolSize;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final List<String> problematicTickers = List.of("ZTST", "IGZ");

    @Scheduled(initialDelay = 150000, fixedRate = 300000) // 2.5min delay zbog bootstrap data, 5min interval
    @Async
    @Transactional
    public void refreshListings() {
        log.info("---- Starting scheduled listing refresh ----");

        List<Listing> listings = listingRepository.findAll();
        List<Stock> stocks = listings.stream().filter(s -> s instanceof Stock).map(s -> (Stock) s).toList();
        List<ForexPair> forexPairs = listings.stream().filter(f -> f instanceof ForexPair).map(f -> (ForexPair) f).toList();

        refreshInParallel(stocks, this::refreshStock);
        refreshInParallel(forexPairs, this::refreshForex);
        refreshOptions(stocks);

        log.info("---- Finished scheduled listing refresh ----");
    }

    private void refreshStock(Stock stock) {
        if (problematicTickers.contains(stock.getTicker())) {
            log.warn("Skipping problematic stock: {}", stock.getTicker());
            return;
        }

        try {
            StockDto dto = stocksService.getStockData(stock.getTicker());
            if (dto != null && (!dto.getPrice().equals(stock.getPrice()) || dto.getVolume() != stock.getVolume())) {
                stock.setPrice(dto.getPrice());
                stock.setVolume(dto.getVolume());
                stock.setChange(dto.getChange());
                listingRepository.save(stock);
            }

            TimeSeriesDto series = listingService.getPriceHistoryFromAlphaVantage(stock.getTicker(), "5min", "compact");
            Set<LocalDateTime> existing = priceHistoryRepository.findDatesByListingId(stock.getId());
            List<ListingPriceHistory> history = createNewHistory(stock, series, existing);
            saveInBatches(history, 100, priceHistoryRepository::saveAllAndFlush);

        } catch (Exception e) {
            log.error("Failed to refresh stock {}", stock.getTicker(), e);
        }
    }

    private void refreshForex(ForexPair forex) {
        try {
            if (forex.getTicker() == null || !forex.getTicker().contains("/")) {
                log.warn("Skipping invalid forex ticker: {}", forex.getTicker());
                return;
            }

            String[] parts = forex.getTicker().split("/");
            if (parts.length != 2) {
                log.warn("Skipping malformed forex ticker: {}", forex.getTicker());
                return;
            }

            ForexPairDto dto = forexService.getForexPair(parts[0], parts[1]);
            if (dto != null && !dto.getPrice().equals(forex.getPrice())) {
                forex.setPrice(dto.getPrice());
                forex.setLiquidity(dto.getLiquidity());
                forex.setExchangeRate(dto.getExchangeRate());
                forex.setLastRefresh(dto.getLastRefresh());
                listingRepository.save(forex);
            }

            TimeSeriesDto series = listingService.getForexPriceHistory(forex.getId(), "5min");
            Set<LocalDateTime> existing = priceHistoryRepository.findDatesByListingId(forex.getId());
            List<ListingPriceHistory> history = createNewHistory(forex, series, existing);
            saveInBatches(history, 100, priceHistoryRepository::saveAllAndFlush);

        } catch (Exception e) {
            log.error("Failed to refresh forex {}", forex.getTicker(), e);
        }
    }

    private void refreshOptions(List<Stock> stocks) {
        log.info("Refreshing options...");

        try {
            Set<String> usedOptionTickers = portfolioEntryRepository.findAll().stream()
                    .filter(e -> e.getType() == ListingType.OPTION)
                    .map(e -> e.getListing().getTicker())
                    .collect(Collectors.toSet());

            List<Option> allOptions = optionRepository.findAll();
            List<Option> toDelete = allOptions.stream()
                    .filter(opt -> !usedOptionTickers.contains(opt.getTicker()))
                    .peek(opt -> opt.setOffer(null))
                    .toList();

            optionRepository.saveAllAndFlush(toDelete);
            optionRepository.deleteAll(toDelete);

            log.info("Deleted {} unused options.", toDelete.size());

            Set<String> tickersToRegenerate = toDelete.stream()
                    .map(opt -> opt.getUnderlyingStock().getTicker())
                    .collect(Collectors.toSet());

            List<Stock> stocksToRegenerate = stocks.stream()
                    .filter(s -> tickersToRegenerate.contains(s.getTicker()))
                    .toList();

            List<Option> newOptions = refreshInParallel(stocksToRegenerate, stock -> {
                try {
                    List<OptionDto> dtos = optionService.generateOptions(stock.getTicker(), stock.getPrice());
                    return dtos.stream()
                            .filter(dto -> !optionRepository.existsByTicker(dto.getTicker()))
                            .map(dto -> {
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
                                return o;
                            }).toList();
                } catch (Exception e) {
                    log.error("Failed to generate options for stock {}", stock.getTicker(), e);
                    return List.of();
                }
            });

            saveInBatches(newOptions, 100, optionRepository::saveAllAndFlush);
            log.info("Inserted {} new options.", newOptions.size());

        } catch (Exception e) {
            log.error("Failed refreshing options", e);
        }
    }

    private List<ListingPriceHistory> createNewHistory(Listing listing, TimeSeriesDto dto, Set<LocalDateTime> existingDates) {
        return dto.getValues().stream()
                .map(value -> {
                    LocalDateTime date = LocalDateTime.parse(value.getDatetime(), formatter);
                    if (existingDates.contains(date)) return null;
                    return ListingPriceHistory.builder()
                            .listing(listing)
                            .date(date)
                            .open(value.getOpen())
                            .high(value.getHigh())
                            .low(value.getLow())
                            .close(value.getClose())
                            .volume(value.getVolume())
                            .change(value.getClose().subtract(value.getOpen()))
                            .build();
                }).filter(Objects::nonNull).toList();
    }

    private <T> void saveInBatches(List<T> items, int batchSize, Consumer<List<T>> saver) {
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            saver.accept(items.subList(i, end));
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
            executor.awaitTermination(10, TimeUnit.MINUTES);
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
}
