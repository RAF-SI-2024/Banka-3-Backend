package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface ListingPriceHistoryRepository extends JpaRepository<ListingPriceHistory, Long> {
    ListingPriceHistory findTopByListingOrderByDateDesc(Listing listing);

    List<ListingPriceHistory> findAllByListingOrderByDateDesc(Listing listing);
    boolean existsByListingAndDate(Listing listing, LocalDateTime date);

    @Query("SELECT l.date FROM ListingPriceHistory l WHERE l.listing.id = :listingId")
    Set<LocalDateTime> findDatesByListingId(@Param("listingId") Long listingId);

}
