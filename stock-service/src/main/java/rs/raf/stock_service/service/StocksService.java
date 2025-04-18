package rs.raf.stock_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.dto.StockSearchDto;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.exceptions.StocksNotFoundException;
import rs.raf.stock_service.exceptions.SymbolSearchException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class StocksService {

    private final AlphavantageClient alphavantageClient;
    private final TwelveDataClient twelveDataClient;
    private final ExchangeService exchangeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<StockSearchDto> searchByTicker(String keyword) {
        try {
            String response = alphavantageClient.searchByTicker(keyword);
            JsonNode root = objectMapper.readTree(response);
            JsonNode bestMatches = root.path("bestMatches");
            List<StockSearchDto> result = new ArrayList<>();
            if (bestMatches.isArray()) {
                for (JsonNode node : bestMatches) {
                    StockSearchDto dto = new StockSearchDto();
                    dto.setTicker(node.path("1. symbol").asText());
                    dto.setName(node.path("2. name").asText());
                    dto.setRegion(node.path("4. region").asText());
                    dto.setMatchScore(node.path("9. matchScore").asText());
                    result.add(dto);
                }
            }
            return result;
        } catch (Exception e) {
            throw new SymbolSearchException("Error searching for symbol with keyword '" + keyword + "': " + e.getMessage());
        }
    }

    @Transactional
    public StockDto getStockData(String symbol) {
        try {
            String quoteResponse = alphavantageClient.getGlobalQuote(symbol);
            JsonNode quoteRoot = objectMapper.readTree(quoteResponse);
            JsonNode globalQuote = quoteRoot.path("Global Quote");

            String priceStr = globalQuote.path("05. price").asText();
            String changeStr = globalQuote.path("09. change").asText();
            String highStr = globalQuote.path("03. high").asText();

            BigDecimal price = priceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(priceStr);
            BigDecimal change = changeStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(changeStr);
            BigDecimal high = highStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(highStr);
            long volume = globalQuote.path("06. volume").asLong();

            String overviewResponse = alphavantageClient.getCompanyOverview(symbol);

            JsonNode overviewRoot = objectMapper.readTree(overviewResponse);

            if (overviewRoot.has("Error Message")){
                return null;
            }

            long outstandingShares = overviewRoot.path("SharesOutstanding").asLong();
            String dividendYieldStr = overviewRoot.path("DividendYield").asText();
            BigDecimal dividendYield = (dividendYieldStr.isEmpty() || dividendYieldStr.equalsIgnoreCase("none")) ? BigDecimal.ZERO : new BigDecimal(dividendYieldStr);
            String name = overviewRoot.path("Name").asText();

            String micCode = overviewRoot.path("Exchange").asText();
            Exchange exchange = exchangeService.getAvailableExchanges()
                    .stream()
                    .filter(e -> e.getMic().equalsIgnoreCase(micCode))
                    .findFirst()
                    .orElse(null);


            BigDecimal marketCap = BigDecimal.valueOf(outstandingShares).multiply(price);
            BigDecimal maintenanceMargin = price.multiply(BigDecimal.valueOf(0.5));

            Stock stock = new Stock();
            stock.setPrice(price);
            stock.setChange(change);
            stock.setVolume(volume);
            stock.setOutstandingShares(outstandingShares);
            stock.setDividendYield(dividendYield);
            stock.setMarketCap(marketCap);
            stock.setName(name);
            stock.setTicker(symbol);
            stock.setMaintenanceMargin(maintenanceMargin);
            stock.setExchange(exchange);
            stock.setAsk(high);
            //stock.setCurrencyCode(exchange.getCurrencyCode());

            return mapToDto(stock);
        } catch (Exception e) {
            throw new StockNotFoundException("Stock data not found for symbol '" + symbol + "': " + e.getMessage());
        }
    }

    @Transactional
    public List<StockDto> getRealtimeBulkStockData(List<String> symbols) {
        try {
            String symbolsJoined = String.join(",", symbols);
            String response = alphavantageClient.getRealtimeBulkQuotes(symbolsJoined);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("message") || root.has("Note") || root.has("Error Message")) {
                throw new RuntimeException("API error or rate limit reached: " + root.path("message").asText(root.toString()));
            }

            JsonNode quotesArray = root.path("data");

            List<StockDto> stockDtos = new ArrayList<>();

            if (quotesArray.isArray()) {
                for (JsonNode quoteNode : quotesArray) {
                    String ticker = quoteNode.path("symbol").asText();
                    BigDecimal price = new BigDecimal(quoteNode.path("close").asText());
                    long volume = quoteNode.path("volume").asLong();
                    BigDecimal change = new BigDecimal(quoteNode.path("change").asText());

                    // Overview dodatni podaci
                    String overviewResponse = alphavantageClient.getCompanyOverview(ticker);
                    JsonNode overviewRoot = objectMapper.readTree(overviewResponse);

                    String name = overviewRoot.path("Name").asText(ticker);
                    long outstandingShares = overviewRoot.path("SharesOutstanding").asLong(0L);
                    String dividendYieldStr = overviewRoot.path("DividendYield").asText("0");
                    BigDecimal dividendYield = dividendYieldStr.equals("None") ? BigDecimal.ZERO : new BigDecimal(dividendYieldStr);
                    String micCode = overviewRoot.path("Exchange").asText("");

                    Exchange exchange = exchangeService.getAvailableExchanges()
                            .stream()
                            .filter(e -> e.getMic().equalsIgnoreCase(micCode))
                            .findFirst()
                            .orElse(null);

                    BigDecimal marketCap = BigDecimal.valueOf(outstandingShares).multiply(price);
                    BigDecimal maintenanceMargin = price.multiply(BigDecimal.valueOf(0.5));

                    Stock stock = new Stock();
                    stock.setTicker(ticker);
                    stock.setPrice(price);
                    stock.setVolume(volume);
                    stock.setName(name);
                    stock.setChange(change);
                    stock.setOutstandingShares(outstandingShares);
                    stock.setDividendYield(dividendYield);
                    stock.setMarketCap(marketCap);
                    stock.setMaintenanceMargin(maintenanceMargin);
                    stock.setExchange(exchange);

                    stockDtos.add(mapToDto(stock));
                }
            }

            return stockDtos;

        } catch (Exception e) {
            throw new StockNotFoundException("Bulk quotes fetch failed: " + e.getMessage());
        }
    }

    private List<StockDto> getStocksList() {
        try {
            String response = twelveDataClient.getAllStocks("");
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            List<Stock> stocks = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode node : data) {
                    Stock stock = new Stock();
                    stock.setName(node.path("name").asText());
                    stock.setTicker(node.path("mic_code").asText());
                    stocks.add(stock);
                }
            }
            List<StockDto> dtos = new ArrayList<>();
            for (Stock s : stocks) {
                dtos.add(mapToDto(s));
            }
            return dtos;
        } catch (Exception e) {
            throw new StocksNotFoundException("Error retrieving stocks list: " + e.getMessage());
        }
    }

    public Page<StockDto> getStocksList(Pageable pageable) {
        List<StockDto> dtos = getStocksList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<StockDto> pagedList = dtos.subList(start, end);
        return new PageImpl<>(pagedList, pageable, dtos.size());
    }

    private StockDto mapToDto(Stock stock) {
        StockDto dto = new StockDto();
        dto.setName(stock.getName());
        dto.setTicker(stock.getTicker());
        dto.setPrice(stock.getPrice());
        dto.setChange(stock.getChange());
        dto.setVolume(stock.getVolume());
        dto.setOutstandingShares(stock.getOutstandingShares());
        dto.setDividendYield(stock.getDividendYield());
        dto.setMarketCap(stock.getMarketCap());
        dto.setMaintenanceMargin(stock.getMaintenanceMargin());
        dto.setExchange(String.valueOf(stock.getExchange()));
        dto.setAsk(stock.getAsk());
        return dto;
    }
}
