package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtcOfferRepository extends JpaRepository<OtcOffer, Long> {

    // Aktivne ponude koje je korisnik primio (prodavac)
    List<OtcOffer> findAllBySellerIdAndStatus(Long sellerId, OtcOfferStatus status);

    // Aktivne ponude koje je korisnik poslao (kupac)
    List<OtcOffer> findAllByBuyerIdAndStatus(Long buyerId, OtcOfferStatus status);
}
