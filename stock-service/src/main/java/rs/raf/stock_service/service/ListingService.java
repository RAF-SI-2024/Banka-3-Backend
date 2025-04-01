package rs.raf.stock_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingDailyPriceInfo;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.TimeSeriesMapper;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingDailyPriceInfoRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OptionRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.specification.ListingSpecification;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ListingService {
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private ListingDailyPriceInfoRepository dailyPriceInfoRepository;

    private TwelveDataClient twelveDataClient;

    @Autowired
    private TimeSeriesMapper timeSeriesMapper;

    @Autowired
    private ListingMapper listingMapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final UserClient userClient;

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

        List<ListingDailyPriceInfo> priceHistory = dailyPriceInfoRepository.findAllByListingOrderByDateDesc(listing);

        ListingDetailsDto dto = listingMapper.toDetailsDto(listing, priceHistory);

        if (listing instanceof Stock) {
            List<LocalDate> optionDates = optionRepository.findAllByStock(listing).stream()
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

}
