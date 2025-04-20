package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.enums.OtcOptionStatus;
import rs.raf.stock_service.domain.enums.TrackedPaymentType;
import rs.raf.stock_service.domain.mapper.OtcOfferMapper;
import rs.raf.stock_service.domain.mapper.OtcOptionMapper;
import rs.raf.stock_service.domain.mapper.TrackedPaymentMapper;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.repository.OtcOfferRepository;
import rs.raf.stock_service.repository.OtcOptionRepository;
import rs.raf.stock_service.repository.PortfolioEntryRepository;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final OtcOptionRepository otcOptionRepository;
    private final OtcOptionMapper otcOptionMapper;
    private final PortfolioService portfolioService;
    private final BankClient bankClient;
    private final TrackedPaymentService trackedPaymentService;

    @Transactional
    public TrackedPaymentDto exerciseOption(Long otcOptionId, Long userId) {
        OtcOption otcOption = otcOptionRepository.findById(otcOptionId)
                .orElseThrow(() -> new OtcOptionNotFoundException(otcOptionId));

        if (!otcOption.getBuyerId().equals(userId))
            throw new UnauthorizedOtcAccessException();

        if (otcOption.getStatus() == OtcOptionStatus.USED)
            throw new OtcOptionAlreadyExercisedException();

        if (otcOption.getStatus() == OtcOptionStatus.EXPIRED || otcOption.getSettlementDate().isBefore(LocalDate.now()))
            throw new OtcOptionSettlementExpiredException();

        BigDecimal totalAmount = otcOption.getStrikePrice().multiply(BigDecimal.valueOf(otcOption.getAmount()));

        String senderAccount = bankClient.getUSDAccountNumberByClientId(otcOption.getBuyerId()).getBody();
        String receiverAccount = bankClient.getUSDAccountNumberByClientId(otcOption.getSellerId()).getBody();

        if (senderAccount == null) {
            throw new OtcAccountNotFoundForBuyerException();
        }

        if (receiverAccount == null) {
            throw new OtcAccountNotFoundForSellerException();
        }

        TrackedPayment trackedPayment = trackedPaymentService.createTrackedPayment(
                otcOptionId,
                TrackedPaymentType.OTC_EXERCISE
        );

        log.info("Created tracked payment {}", trackedPayment);

        CreatePaymentDto createPaymentDto = CreatePaymentDto
                .builder()
                .amount(totalAmount)
                .senderAccountNumber(senderAccount)
                .receiverAccountNumber(receiverAccount)
                .callbackId(trackedPayment.getId())
                .purposeOfPayment("OTC Exercise Option")
                .paymentCode("289")
                .build();

        bankClient.executeSystemPayment(
                ExecutePaymentDto.builder()
                        .clientId(userId)
                        .createPaymentDto(createPaymentDto)
                        .build()
        );

        return TrackedPaymentMapper.toDto(trackedPayment);
    }

    public void handleExerciseSuccessfulPayment(Long trackedPaymentId) {
        TrackedPayment trackedPayment = trackedPaymentService.getTrackedPayment(trackedPaymentId);
        Long otcOptionId = trackedPayment.getTrackedEntityId();

        OtcOption otcOption = otcOptionRepository.findById(otcOptionId)
                .orElseThrow(() -> new OtcOptionNotFoundException(otcOptionId));

        Stock stock = otcOption.getUnderlyingStock();

        if (otcOption.getStatus() == OtcOptionStatus.USED)
            throw new OtcOptionAlreadyExercisedException();

        if (otcOption.getStatus() == OtcOptionStatus.EXPIRED || otcOption.getSettlementDate().isBefore(LocalDate.now()))
            throw new OtcOptionSettlementExpiredException();

        portfolioService.updateHoldingsOnOtcOptionExecution(
                otcOption.getSellerId(),
                otcOption.getBuyerId(),
                stock,
                otcOption.getAmount(),
                otcOption.getStrikePrice()
        );

        OtcOffer offer = otcOption.getOtcOffer();
        offer.setStatus(OtcOfferStatus.EXERCISED);
        otcOfferRepository.save(offer);

        otcOption.setStatus(OtcOptionStatus.USED);
        otcOptionRepository.save(otcOption);
    }

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

        PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(offer.getSellerId(),
                offer.getStock()).orElseThrow(PortfolioEntryNotFoundException::new);

        if (portfolioEntry.getPublicAmount() < offer.getAmount())
            throw new PortfolioAmountNotEnoughException(portfolioEntry.getPublicAmount(), offer.getAmount());

        //Dodati payment za premium, cekam izmene payment service-a, za sad nastavi kao da je payment prosao

        portfolioEntry.setPublicAmount(portfolioEntry.getPublicAmount() - offer.getAmount());
        portfolioEntry.setReservedAmount(portfolioEntry.getReservedAmount() + offer.getAmount());
        portfolioEntryRepository.save(portfolioEntry);

        OtcOption otcOption = OtcOption.builder()
                .otcOffer(offer)
                .sellerId(offer.getSellerId())
                .buyerId(offer.getBuyerId())
                .underlyingStock(offer.getStock())
                .strikePrice(offer.getPricePerStock())
                .amount(offer.getAmount())
                .settlementDate(offer.getSettlementDate())
                .premium(offer.getPremium())
                .status(OtcOptionStatus.VALID)
                .build();
        otcOptionRepository.save(otcOption);

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

    public List<OtcOptionDto> getOtcOptionsForUser(Boolean valid, Long userId) {
        LocalDate today = LocalDate.now();
        List<OtcOption> options;

        if (valid == null) {
            options = otcOptionRepository.findAllByBuyerId(userId);
        } else if (valid) {
            options = otcOptionRepository.findAllValid(userId, today);
        } else {
            options = otcOptionRepository.findAllInvalid(userId, today);
        }

        return options.stream()
                .map(option -> {
                    String ownerName = resolveUserName(option.getSellerId());
                    return otcOptionMapper.toDto(option, ownerName);
                })
                .collect(Collectors.toList());
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

    @Scheduled()
    private void checkOtcOptionExpiration(){
        List<OtcOption> otcOptions =  otcOptionRepository.findAllValidButExpired(LocalDate.now());

        for (OtcOption otcOption : otcOptions){
            PortfolioEntry portfolioEntry = portfolioEntryRepository.findByUserIdAndListing(otcOption.getSellerId(),
                    otcOption.getUnderlyingStock()).orElseThrow(PortfolioEntryNotFoundException::new);

            portfolioEntry.setAmount(portfolioEntry.getAmount() + otcOption.getAmount());

            otcOption.setStatus(OtcOptionStatus.EXPIRED);
            otcOptionRepository.save(otcOption);
        }
    }
}
