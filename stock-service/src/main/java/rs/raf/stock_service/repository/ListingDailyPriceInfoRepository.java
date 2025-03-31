package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;

import java.util.List;

@Repository
public interface ListingDailyPriceInfoRepository extends JpaRepository<ListingPriceHistory, Long> {
    ListingPriceHistory findTopByListingOrderByDateDesc(Listing listing);

    List<ListingPriceHistory> findAllByListingOrderByDateDesc(Listing listing);
}
