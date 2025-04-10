package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.entity.PortfolioEntry;


import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PortfolioEntryRepository extends JpaRepository<PortfolioEntry, Long> {

    List<PortfolioEntry> findAllByUserId(Long userId);

    Optional<PortfolioEntry> findByUserIdAndListing(Long userId, Listing listing);

    void deleteByUserIdAndListing(Long userId, Listing listing);

    Optional<PortfolioEntry> findByUserIdAndListingId(Long userId, Long listingId);

    List<PortfolioEntry> findAllByTypeAndPublicAmountGreaterThan(ListingType type, Integer minAmount);

    Optional<PortfolioEntry> findByUserIdAndId(Long userId, Long entryId);

    @Query("SELECT e.listing.ticker FROM PortfolioEntry e WHERE e.type = 'OPTION'")
    Set<String> findAllOptionTickersInUse();
}
