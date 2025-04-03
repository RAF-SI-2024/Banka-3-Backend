package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.enums.ListingType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioEntryRepository extends JpaRepository<PortfolioEntry, Long> {

    List<PortfolioEntry> findAllByUserId(Long userId);

    Optional<PortfolioEntry> findByUserIdAndListing(Long userId, Listing listing);

    void deleteByUserIdAndListing(Long userId, Listing listing);

    Optional<PortfolioEntry> findByUserIdAndListingId(Long userId, Long listingId);

    List<PortfolioEntry> findAllByTypeAndPublicAmountGreaterThan(ListingType type, Integer minAmount);

    Optional<PortfolioEntry> findByUserIdAndId(Long userId, Long entryId);
}
