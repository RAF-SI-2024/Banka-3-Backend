package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.CreateOtcOfferDto;
import rs.raf.stock_service.domain.dto.OtcOfferDto;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.mapper.OtcOfferMapper;
import rs.raf.stock_service.exceptions.InvalidPublicAmountException;
import rs.raf.stock_service.exceptions.PortfolioEntryNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedActionException;
import rs.raf.stock_service.repository.OtcOfferRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class OtcService {

    private final OtcOfferRepository otcOfferRepository;
    private final PortfolioEntryRepository portfolioEntryRepository;
    private final OtcOfferMapper otcOfferMapper;


    ///Klijent šalje novu ponudu

    public OtcOfferDto createOffer(CreateOtcOfferDto dto, Long buyerId) {
        PortfolioEntry sellerEntry = portfolioEntryRepository.findById(dto.getPortfolioEntryId())
                .orElseThrow(PortfolioEntryNotFoundException::new);

        Stock stock = (Stock) sellerEntry.getListing();
        Long sellerId = sellerEntry.getUserId();

        if (dto.getAmount().compareTo(BigDecimal.valueOf(sellerEntry.getPublicAmount())) > 0) {
            throw new InvalidPublicAmountException("Not enough public shares to fulfill the offer");
        }

        OtcOffer offer = OtcOffer.builder()
                .stock(stock)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(dto.getAmount().intValue())
                .pricePerStock(dto.getPricePerStock())
                .premium(dto.getPremium())
                .settlementDate(dto.getSettlementDate())
                .status(OtcOfferStatus.PENDING)
                .lastModified(LocalDateTime.now())
                .lastModifiedById(buyerId)
                .build();

        return otcOfferMapper.toDto(otcOfferRepository.save(offer));
    }




    ///	Prodavac vidi sve aktivne ponude
    public List<OtcOfferDto> getAllActiveOffersForSeller(Long sellerId) {
        return otcOfferRepository.findAllBySellerIdAndStatus(sellerId, OtcOfferStatus.PENDING)
                .stream()
                .map(otcOfferMapper::toDto)
                .collect(Collectors.toList());
    }


    /// Prodavac prihvata ponudu
    @Transactional
    public void acceptOffer(Long offerId, Long userId) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        offer.setStatus(OtcOfferStatus.ACCEPTED);
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        otcOfferRepository.save(offer);
    }



    ///	Prodavac odbija ponudu
    @Transactional
    public void rejectOffer(Long offerId, Long userId) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        offer.setStatus(OtcOfferStatus.REJECTED);
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        otcOfferRepository.save(offer);
    }



    /// Prodavac šalje kontra ponudu
    @Transactional
    public void updateOffer(Long offerId, Long userId, CreateOtcOfferDto dto) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        offer.setAmount(dto.getAmount().intValue());
        offer.setPricePerStock(dto.getPricePerStock());
        offer.setPremium(dto.getPremium());
        offer.setSettlementDate(dto.getSettlementDate());
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        offer.setStatus(OtcOfferStatus.PENDING);
        otcOfferRepository.save(offer);
    }
}

