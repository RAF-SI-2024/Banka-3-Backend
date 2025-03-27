package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingDailyPriceInfo;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.enums.OrderDirection;
import rs.raf.stock_service.domain.enums.OrderStatus;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingDailyPriceInfoRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.specification.ListingSpecification;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
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

    @Autowired
    private ListingMapper listingMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

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

        return listingMapper.toDetailsDto(listing, priceHistory);
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
}
