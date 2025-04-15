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
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ListingService {

    @Autowired private ListingRepository listingRepository;
    @Autowired private ListingPriceHistoryRepository dailyPriceInfoRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private ListingMapper listingMapper;
    @Autowired private JwtTokenUtil jwtTokenUtil;
    @Autowired private ListingRedisService listingRedisService;
    @Autowired private TimeSeriesMapper timeSeriesMapper;
    @Autowired private TwelveDataClient twelveDataClient;
    @Autowired private AlphavantageClient alphavantageClient;

    public ListingDto getByTicker(String ticker) {
        ListingDto cached = listingRedisService.getByTicker(ticker);
        if (cached != null) {
            return cached;
        }

        Listing listing = listingRepository.findByTicker(ticker)
                .orElseThrow(() -> new ListingNotFoundException(ticker));

        ListingPriceHistory dailyInfo = dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing);
        ListingDto dto = listingMapper.toDto(listing, dailyInfo);

        listingRedisService.saveByTicker(dto);
        return dto;
    }

    public List<ListingDto> getListings(ListingFilterDto filter, String role) {
        var spec = ListingSpecification.buildSpecification(filter, role);

        if (filter == null) {
            return listingRedisService.getAllListings();
        }

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

        ListingPriceHistory dailyInfo = dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing);
        ListingDto updatedDto = listingMapper.toDto(listing, dailyInfo);

        listingRedisService.saveByTicker(updatedDto);
        return updatedDto;
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
        String response = alphavantageClient.getIntradayData(symbol, interval, outputsize, "json");
        return mapAlphaVantageResponseToDto(response, symbol, interval);
    }

    private TimeSeriesDto mapAlphaVantageResponseToDto(String response, String symbol, String interval) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

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

    public TimeSeriesDto getForexPriceHistory(Long id, String interval) {
        ForexPair forexPair = (ForexPair) listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        String fromSymbol = forexPair.getBaseCurrency();
        String toSymbol = forexPair.getQuoteCurrency();

        String response = alphavantageClient.getForexPriceHistory(fromSymbol, toSymbol, interval, "compact");
        return timeSeriesMapper.mapJsonToCustomTimeSeries(response, forexPair);
    }

}
