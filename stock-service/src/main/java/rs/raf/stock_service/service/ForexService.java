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
import rs.raf.stock_service.client.ExchangeRateApiClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.ForexPairDto;
import rs.raf.stock_service.domain.entity.ForexPair;
import rs.raf.stock_service.exceptions.ExchangeRateConversionException;
import rs.raf.stock_service.exceptions.ForexPairNotFoundException;
import rs.raf.stock_service.exceptions.ForexPairsNotFoundException;
import rs.raf.stock_service.exceptions.LatestRatesNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@AllArgsConstructor
public class ForexService {

    private final AlphavantageClient alphavantageClient;
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final TwelveDataClient twelveDataClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ForexPairDto getForexPair(String fromCurrency, String toCurrency) {
        try {
            String response = alphavantageClient.getCurrencyExchangeRate(fromCurrency, toCurrency);
            JsonNode root = objectMapper.readTree(response);
            JsonNode rateNode = root.path("Realtime Currency Exchange Rate");

            String exchangeRateStr = rateNode.path("5. Exchange Rate").asText();
            BigDecimal exchangeRate = exchangeRateStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(exchangeRateStr);

            String askPriceStr = rateNode.path("9. Ask Price").asText();
            BigDecimal askPrice = askPriceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(askPriceStr);

            String bidPriceStr = rateNode.path("8. Bid Price").asText();
            BigDecimal bidPrice = bidPriceStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(bidPriceStr);

            String liquidity = "Medium";
            int contractSize = 1000;
            BigDecimal maintenanceMargin = BigDecimal.valueOf(contractSize)
                    .multiply(exchangeRate)
                    .multiply(BigDecimal.valueOf(0.10));
            BigDecimal nominalValue = BigDecimal.valueOf(contractSize).multiply(exchangeRate);
            LocalDateTime lastRefresh = LocalDateTime.parse(
                    rateNode.path("6. Last Refreshed").asText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );

            ForexPair forexPair = new ForexPair();
            forexPair.setName(fromCurrency + "/" + toCurrency);
            forexPair.setTicker(fromCurrency + "/" + toCurrency);
            forexPair.setBaseCurrency(fromCurrency);
            forexPair.setQuoteCurrency(toCurrency);
            forexPair.setExchangeRate(exchangeRate);
            forexPair.setLiquidity(liquidity);
            forexPair.setLastRefresh(lastRefresh);
            forexPair.setMaintenanceMargin(maintenanceMargin);
            forexPair.setNominalValue(nominalValue);
            forexPair.setAsk(askPrice);
            forexPair.setPrice(bidPrice);

            return mapToDto(forexPair);
        } catch (Exception e) {
            throw new ForexPairNotFoundException("Forex pair data not found for " + fromCurrency + "/" + toCurrency + ": " + e.getMessage());
        }
    }

    public BigDecimal getConversionRate(String base, String target) {
        try {
            String response = exchangeRateApiClient.getConversionPair("", base, target);
            JsonNode root = objectMapper.readTree(response);
            String rateStr = root.path("conversion_rate").asText();
            return rateStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(rateStr);
        } catch (Exception e) {
            throw new ExchangeRateConversionException("Error converting from " + base + " to " + target + ": " + e.getMessage());
        }
    }

    public Map<String, BigDecimal> getLatestRates(String base) {
        try {
            String response = exchangeRateApiClient.getLatestRates("", base);
            JsonNode root = objectMapper.readTree(response);
            JsonNode conversionRatesNode = root.path("conversion_rates");
            Map<String, BigDecimal> rates = new HashMap<>();
            if (conversionRatesNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = conversionRatesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String currency = entry.getKey();
                    String valueStr = entry.getValue().asText();
                    BigDecimal value = valueStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(valueStr);
                    rates.put(currency, value);
                }
            }
            return rates;
        } catch (Exception e) {
            throw new LatestRatesNotFoundException("Latest rates not found for base currency " + base + ": " + e.getMessage());
        }
    }

    private List<ForexPairDto> getForexPairsList() {
        try {
            String response = twelveDataClient.getAllForexPairs("");
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            List<ForexPair> forexPairs = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode node : data) {
                    ForexPair forex = new ForexPair();
                    String symbol = node.path("symbol").asText();
                    if (symbol.contains("/")) {
                        String[] parts = symbol.split("/");
                        forex.setBaseCurrency(parts[0]);
                        forex.setQuoteCurrency(parts[1]);
                    } else {
                        forex.setBaseCurrency("");
                        forex.setQuoteCurrency("");
                        forex.setName(symbol);
                    }
                    forexPairs.add(forex);
                }
            }
            List<ForexPairDto> dtos = new ArrayList<>();
            for (ForexPair fp : forexPairs) {
                dtos.add(mapToDto(fp));
            }
            return dtos;
        } catch (Exception e) {
            throw new ForexPairsNotFoundException("Error retrieving forex pairs list: " + e.getMessage());
        }
    }

    public Page<ForexPairDto> getForexPairsList(Pageable pageable) {
        List<ForexPairDto> dtos = getForexPairsList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<ForexPairDto> pagedList = dtos.subList(start, end);
        return new PageImpl<>(pagedList, pageable, dtos.size());
    }

    private ForexPairDto mapToDto(ForexPair forex) {
        ForexPairDto dto = new ForexPairDto();
        dto.setName(forex.getName());
        dto.setTicker(forex.getTicker());
        dto.setBaseCurrency(forex.getBaseCurrency());
        dto.setQuoteCurrency(forex.getQuoteCurrency());
        dto.setExchangeRate(forex.getExchangeRate());
        dto.setLiquidity(forex.getLiquidity());
        dto.setLastRefresh(forex.getLastRefresh());
        dto.setMaintenanceMargin(forex.getMaintenanceMargin());
        dto.setNominalValue(forex.getNominalValue());
        dto.setAsk(forex.getAsk());
        dto.setPrice(forex.getPrice());
        return dto;
    }
}
