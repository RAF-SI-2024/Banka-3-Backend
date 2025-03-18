package rs.raf.stock_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.dto.StockSearchDto;
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
                    dto.setSymbol(node.path("1. symbol").asText());
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

    public StockDto getStockData(String symbol) {
        try {
            String quoteResponse = alphavantageClient.getGlobalQuote(symbol);
            JsonNode quoteRoot = objectMapper.readTree(quoteResponse);
            JsonNode globalQuote = quoteRoot.path("Global Quote");

            String priceStr = globalQuote.path("05. price").asText();
            String changeStr = globalQuote.path("09. change").asText();
            BigDecimal price = priceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(priceStr);
            BigDecimal change = changeStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(changeStr);
            long volume = globalQuote.path("06. volume").asLong();

            String overviewResponse = alphavantageClient.getCompanyOverview(symbol);
            JsonNode overviewRoot = objectMapper.readTree(overviewResponse);
            long outstandingShares = overviewRoot.path("SharesOutstanding").asLong();
            String dividendYieldStr = overviewRoot.path("DividendYield").asText();
            BigDecimal dividendYield = dividendYieldStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(dividendYieldStr);
            String name = overviewRoot.path("Name").asText();
            String exchange = overviewRoot.has("Exchange") ? overviewRoot.path("Exchange").asText() : "N/A";

            BigDecimal marketCap = BigDecimal.valueOf(outstandingShares).multiply(price);
            BigDecimal maintenanceMargin = price.multiply(BigDecimal.valueOf(0.5));

            Stock stock = new Stock();
            stock.setSymbol(symbol);
            stock.setPrice(price);
            stock.setChange(change);
            stock.setVolume(volume);
            stock.setOutstandingShares(outstandingShares);
            stock.setDividendYield(dividendYield);
            stock.setMarketCap(marketCap);
            stock.setName(name);
            stock.setTicker(symbol);
            stock.setMaintenanceMargin(maintenanceMargin);
//            stock.setExchange(exchange);

            return mapToDto(stock);
        } catch (Exception e) {
            throw new StockNotFoundException("Stock data not found for symbol '" + symbol + "': " + e.getMessage());
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
                    stock.setSymbol(node.path("symbol").asText());
                    stock.setName(node.path("name").asText());
                    stock.setTicker(node.path("mic_code").asText());
                    // Mapirajte dodatne podatke po potrebi.
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
        dto.setSymbol(stock.getSymbol());
        dto.setName(stock.getName());
        dto.setTicker(stock.getTicker());
        dto.setPrice(stock.getPrice());
        dto.setChange(stock.getChange());
        dto.setVolume(stock.getVolume());
        dto.setOutstandingShares(stock.getOutstandingShares());
        dto.setDividendYield(stock.getDividendYield());
        dto.setMarketCap(stock.getMarketCap());
        dto.setMaintenanceMargin(stock.getMaintenanceMargin());
        // dto.setExchange(stock.getExchange());
        return dto;
    }
}
