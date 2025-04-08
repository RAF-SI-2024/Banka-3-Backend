package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class OtcService {

    private final OtcOfferRepository otcOfferRepository;
    private final PortfolioEntryRepository portfolioEntryRepository;
    private final OtcOfferMapper otcOfferMapper;
    private final UserClient userClient;

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

        return otcOfferMapper.toDto(otcOfferRepository.save(offer), buyerId);
    }

    public List<OtcOfferDto> getAllActiveOffersForUser(Long userId) {
        return otcOfferRepository.findAllByStatus(OtcOfferStatus.PENDING).stream()
                .filter(offer -> offer.getSellerId().equals(userId) || offer.getBuyerId().equals(userId))
                .sorted(Comparator.comparing(OtcOffer::getLastModified).reversed())
                .map(offer -> {
                    OtcOfferDto dto = otcOfferMapper.toDto(offer, userId);

                    boolean canInteract = !offer.getLastModifiedById().equals(userId);
                    dto.setCanInteract(canInteract);

                    Long nameUserId;
                    if (canInteract) {
                        nameUserId = offer.getLastModifiedById(); // Onaj koji je poslednji slao
                    } else {
                        nameUserId = userId.equals(offer.getBuyerId()) ? offer.getSellerId() : offer.getBuyerId(); // druga strana
                    }

                    dto.setName(resolveUserName(nameUserId));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptOffer(Long offerId, Long userId) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        if (!(userId.equals(offer.getSellerId()) || userId.equals(offer.getBuyerId()))
                || offer.getLastModifiedById().equals(userId)) {
            throw new UnauthorizedActionException("Not allowed to update this offer");
        }

        offer.setStatus(OtcOfferStatus.ACCEPTED);
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        otcOfferRepository.save(offer);
    }

    @Transactional
    public void rejectOffer(Long offerId, Long userId) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        if (!(userId.equals(offer.getSellerId()) || userId.equals(offer.getBuyerId()))
                || offer.getLastModifiedById().equals(userId)) {
            throw new UnauthorizedActionException("Not allowed to update this offer");
        }

        offer.setStatus(OtcOfferStatus.REJECTED);
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        otcOfferRepository.save(offer);
    }

    @Transactional
    public void updateOffer(Long offerId, Long userId, CreateOtcOfferDto dto) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        if (!(userId.equals(offer.getSellerId()) || userId.equals(offer.getBuyerId()))
                || offer.getLastModifiedById().equals(userId)) {
            throw new UnauthorizedActionException("Not allowed to update this offer");
        }

        offer.setAmount(dto.getAmount().intValue());
        offer.setPricePerStock(dto.getPricePerStock());
        offer.setPremium(dto.getPremium());
        offer.setSettlementDate(dto.getSettlementDate());
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        offer.setStatus(OtcOfferStatus.PENDING);
        otcOfferRepository.save(offer);
    }

    @Transactional
    public void cancelOffer(Long offerId, Long userId) {
        OtcOffer offer = otcOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        //  samo ako je korisnik poslednji modifikovao ponudu
        if (!offer.getLastModifiedById().equals(userId)) {
            throw new UnauthorizedActionException("Not allowed to cancel this offer");
        }

        offer.setStatus(OtcOfferStatus.CANCELLED);
        offer.setLastModified(LocalDateTime.now());
        offer.setLastModifiedById(userId);
        otcOfferRepository.save(offer);
    }


    private String resolveUserName(Long userId) {
        try {
            ClientDto client = userClient.getClientById(userId);
            return formatName(client.getFirstName(), client.getLastName());
        } catch (Exception e1) {
            try {
                ActuaryDto actuary = userClient.getEmployeeById(userId);
                return formatName(actuary.getFirstName(), actuary.getLastName());
            } catch (Exception e2) {
                return "Unknown User";
            }
        }
    }

    private String formatName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return "Unknown User";
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}

