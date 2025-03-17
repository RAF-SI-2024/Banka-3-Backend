package rs.raf.stock_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.ListingFilterDto;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.repository.ListingDailyPriceInfoRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.specification.ListingSpecification;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListingService {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingDailyPriceInfoRepository dailyPriceInfoRepository;

    @Autowired
    private ListingMapper listingMapper;

    public List<ListingDto> getListings(ListingFilterDto filter, String role) {
        var spec = ListingSpecification.buildSpecification(filter, role);
        return listingRepository.findAll(spec).stream()
                .map(listing -> listingMapper.toDto(listing, dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)))
                .collect(Collectors.toList());
    }
}
