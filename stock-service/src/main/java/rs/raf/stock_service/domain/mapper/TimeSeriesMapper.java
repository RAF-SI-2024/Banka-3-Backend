package rs.raf.stock_service.domain.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.TimeSeriesDto;
import rs.raf.stock_service.domain.entity.ForexPair;
import rs.raf.stock_service.domain.entity.Listing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@AllArgsConstructor
public class TimeSeriesMapper {

    private final ObjectMapper objectMapper;

    public TimeSeriesDto mapJsonToCustomTimeSeries(String jsonResponse, Listing listing) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // Provera da li je JSON prazan ili sadrži grešku
            if (rootNode.has("Error Message")) {
                throw new IllegalArgumentException("API Error: " + rootNode.get("Error Message").asText());
            }

            // Dohvatanje time series podataka (prva ključna vrednost)
            String timeSeriesKey = getTimeSeriesKey(rootNode);
            JsonNode timeSeriesData = rootNode.get(timeSeriesKey);

            List<TimeSeriesDto.TimeSeriesValueDto> values = new ArrayList<>();
            for (String datetime : getDateKeys(timeSeriesData)) {
                JsonNode data = timeSeriesData.get(datetime);

                TimeSeriesDto.TimeSeriesValueDto valueDto = new TimeSeriesDto.TimeSeriesValueDto();
                valueDto.setDatetime(datetime);
                valueDto.setOpen(new BigDecimal(data.get("1. open").asText()));
                valueDto.setHigh(new BigDecimal(data.get("2. high").asText()));
                valueDto.setLow(new BigDecimal(data.get("3. low").asText()));
                valueDto.setClose(new BigDecimal(data.get("4. close").asText()));
                valueDto.setVolume(data.has("5. volume") ? data.get("5. volume").asLong() : 0L);

                values.add(valueDto);
            }

            TimeSeriesDto timeSeriesDto = new TimeSeriesDto();
            timeSeriesDto.setValues(values);
            timeSeriesDto.setStatus("success");

            // Dodavanje meta podataka
            timeSeriesDto.setMeta(createMetaData(rootNode, listing));

            return timeSeriesDto;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping JSON to Time Series DTO: " + e.getMessage(), e);
        }
    }

    // Kreiranje meta podataka na osnovu odgovora iz API-ja
    private TimeSeriesDto.MetaDto createMetaData(JsonNode rootNode, Listing listing) {
        TimeSeriesDto.MetaDto metaDto = new TimeSeriesDto.MetaDto();
        JsonNode metaNode = rootNode.path("Meta Data");

        if (!metaNode.isMissingNode()) {
            metaDto.setSymbol(metaNode.path("2. From Symbol").asText() + "/" + metaNode.path("3. To Symbol").asText());
            metaDto.setInterval(metaNode.path("4. Interval").asText());
            metaDto.setCurrency(metaNode.path("5. Output Size").asText());
            metaDto.setType("Forex");
            metaDto.setExchange("Forex Exchange");

            if (listing instanceof ForexPair) {
                metaDto.setCurrency_base(((ForexPair) listing).getBaseCurrency());
                metaDto.setCurrency_quote(((ForexPair) listing).getQuoteCurrency());
            }
        }
        return metaDto;
    }

    // Vraća ključ za time series podatke (npr. "Time Series FX (5min)")
    private String getTimeSeriesKey(JsonNode rootNode) {
        for (Iterator<String> it = rootNode.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            if (key.startsWith("Time Series FX")) {
                return key;
            }
        }
        throw new IllegalArgumentException("Time Series data not found in API response.");
    }

    // Dobijanje ključeva koji predstavljaju datume (timestampove) u JSON-u
    private List<String> getDateKeys(JsonNode timeSeriesData) {
        List<String> dateKeys = new ArrayList<>();
        timeSeriesData.fieldNames().forEachRemaining(dateKeys::add);
        return dateKeys;
    }
}
