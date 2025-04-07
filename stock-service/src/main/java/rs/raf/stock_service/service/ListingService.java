package rs.raf.stock_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.TimeSeriesMapper;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OptionRepository;
import rs.raf.stock_service.specification.ListingSpecification;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ListingService {
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private ListingPriceHistoryRepository dailyPriceInfoRepository;

    private TwelveDataClient twelveDataClient;
    private AlphavantageClient alphavantageClient;

    @Autowired
    private TimeSeriesMapper timeSeriesMapper;

    @Autowired
    private ListingMapper listingMapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private OptionRepository optionRepository;

    public List<ListingDto> getListings(ListingFilterDto filter, String role) {
        var spec = ListingSpecification.buildSpecification(filter, role);
        return listingRepository.findAll(spec).stream()
                .map(listing -> listingMapper.toDto(listing, dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)))
                .collect(Collectors.toList());
    }

    public ListingDetailsDto getListingDetails(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        List<ListingPriceHistory> priceHistory = dailyPriceInfoRepository.findAllByListingOrderByDateDesc(listing);

        ListingDetailsDto dto = listingMapper.toDetailsDto(listing, priceHistory);

        if (listing instanceof Stock) {
            List<LocalDate> optionDates = optionRepository.findAllByUnderlyingStock((Stock) listing).stream()
                    .filter(Option::isOnSale)
                    .map(Option::getSettlementDate)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            dto.setOptionSettlementDates(optionDates);
        }

        return dto;
    }

    public ListingDto updateListing(Long id, ListingUpdateDto updateDto, String authHeader) {

        String role = jwtTokenUtil.getUserRoleFromAuthHeader(authHeader);
        if (!"SUPERVISOR".equals(role) && !"ADMIN".equals(role)) {
            throw new UnauthorizedException("Only supervisors can update listings.");
        }

        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (updateDto.getPrice() != null) listing.setPrice(updateDto.getPrice());
        if (updateDto.getAsk() != null) listing.setAsk(updateDto.getAsk());

        listingRepository.save(listing);

        return listingMapper.toDto(listing, dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing));
    }

    public TimeSeriesDto getPriceHistory(Long id, String interval) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (interval == null || interval.isEmpty()) {
            interval = "1day";
        }

        String response = twelveDataClient.getTimeSeries(listing.getTicker(), interval, "30");

        return timeSeriesMapper.mapJsonToCustomTimeSeries(response, listing);
    }

    public TimeSeriesDto getPriceHistoryFromAlphaVantage(String symbol, String interval, String outputsize) {

        // Call the API
        String response = alphavantageClient.getIntradayData(symbol, interval, outputsize, "json");

        // Map response to TimeSeriesDto
        return mapAlphaVantageResponseToDto(response, symbol, interval);
    }

    // Helper method to map Alpha Vantage response to TimeSeriesDto
    private TimeSeriesDto mapAlphaVantageResponseToDto(String response, String symbol, String interval) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            // Get time series data (e.g., "Time Series (5min)" or other interval)
            JsonNode timeSeriesNode = rootNode.get("Time Series (" + interval + ")");
            if (timeSeriesNode == null) {
                throw new RuntimeException("Invalid response format from Alpha Vantage");
            }

            List<TimeSeriesDto.TimeSeriesValueDto> values = new ArrayList<>();
            for (Iterator<String> it = timeSeriesNode.fieldNames(); it.hasNext(); ) {
                String datetime = it.next();
                JsonNode data = timeSeriesNode.get(datetime);

                TimeSeriesDto.TimeSeriesValueDto dto = new TimeSeriesDto.TimeSeriesValueDto();
                dto.setDatetime(datetime);
                dto.setOpen(new BigDecimal(data.get("1. open").asText()));
                dto.setHigh(new BigDecimal(data.get("2. high").asText()));
                dto.setLow(new BigDecimal(data.get("3. low").asText()));
                dto.setClose(new BigDecimal(data.get("4. close").asText()));
                dto.setVolume(data.get("5. volume").asLong());

                values.add(dto);
            }

            // Create and return TimeSeriesDto
            TimeSeriesDto timeSeriesDto = new TimeSeriesDto();
            TimeSeriesDto.MetaDto metaDto = new TimeSeriesDto.MetaDto();
            metaDto.setSymbol(symbol);
            metaDto.setInterval(interval);
            metaDto.setType("Equity");

            timeSeriesDto.setMeta(metaDto);
            timeSeriesDto.setValues(values);
            timeSeriesDto.setStatus("success");

            return timeSeriesDto;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Alpha Vantage response: " + e.getMessage());
        }
    }

    private void importForexPriceHistory() {
        System.out.println("Fetching intraday price history for all forex pairs...");

        List<ForexPair> forexPairs = listingRepository.findAll().stream()
                .filter(listing -> listing instanceof ForexPair)
                .map(listing -> (ForexPair) listing)
                .collect(Collectors.toList());

        List<ListingPriceHistory> allPriceHistoryEntities = new ArrayList<>();

        for (ForexPair forexPair : forexPairs) {
            try {
                String fromSymbol = forexPair.getBaseCurrency();
                String toSymbol = forexPair.getQuoteCurrency();

                // Poziv API-ja za FX_INTRADAY podatke
                String response = alphavantageClient.getForexPriceHistory(fromSymbol, toSymbol, "5min", "full");

                TimeSeriesDto priceHistory = timeSeriesMapper.mapJsonToCustomTimeSeries(response, forexPair);

                // Dodavanje svih podataka u listu
                allPriceHistoryEntities.addAll(createPriceHistoryEntities(forexPair, priceHistory));
                System.out.println("Fetched price history for forex pair: " + forexPair.getTicker());
            } catch (Exception e) {
                System.err.println("Error fetching price history for forex pair: " + forexPair.getTicker() + " - " + e.getMessage());
            }
        }

        // Batch insert za price history
        if (!allPriceHistoryEntities.isEmpty()) {
            dailyPriceInfoRepository.saveAll(allPriceHistoryEntities);
            System.out.println("Successfully imported price history for " + allPriceHistoryEntities.size() + " forex records.");
        } else {
            System.out.println("No forex price history records to import.");
        }

        System.out.println("Completed importing intraday price history for all forex pairs.");
    }

    // Pomoćna metoda za kreiranje entiteta ListingPriceHistory
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

    public TimeSeriesDto getForexPriceHistory(Long id, String interval) {
        ForexPair forexPair = (ForexPair) listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(1L));

        String fromSymbol = forexPair.getBaseCurrency();
        String toSymbol = forexPair.getQuoteCurrency();

        String response = alphavantageClient.getForexPriceHistory(fromSymbol, toSymbol, interval, "compact");

        return timeSeriesMapper.mapJsonToCustomTimeSeries(response, forexPair);
    }

}
