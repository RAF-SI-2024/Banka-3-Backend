package rs.raf.stock_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ClientDto;
import rs.raf.stock_service.domain.dto.CreatePaymentDto;
import rs.raf.stock_service.domain.dto.ExecutePaymentDto;
import rs.raf.stock_service.domain.dto.PaymentDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.repository.OptionRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.OtcOption;
import rs.raf.stock_service.domain.entity.PortfolioEntry;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.mapper.OtcOfferMapper;
import rs.raf.stock_service.domain.mapper.OtcOptionMapper;
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
    private final OtcOptionRepository optionRepository;
    private final OtcOptionRepository otcOptionRepository;
    private final OtcOptionMapper otcOptionMapper;
    private final PortfolioService portfolioService;
    private final BankClient bankClient;
  
      @Transactional
    public void exerciseOption(Long otcOptionId, Long userId) {
        OtcOption otcOption = otcOptionRepository.findById(otcOptionId)
                .orElseThrow(() -> new OtcOptionNotFoundException(otcOptionId));

        OtcOffer offer = otcOption.getOtcOffer();
        if (offer == null) throw new InvalidOtcOptionException("This is not an OTC option");

        if (!otcOption.getBuyerId().equals(userId))
            throw new UnauthorizedOtcAccessException();

        if (offer.getStatus() == OtcOfferStatus.EXERCISED)
            throw new OtcOptionAlreadyExercisedException();

        if (otcOption.getSettlementDate().isBefore(LocalDate.now()))
            throw new OtcOptionSettlementExpiredException();

        BigDecimal totalAmount = offer.getPricePerStock().multiply(BigDecimal.valueOf(otcOption.getAmount()));
        Stock stock = otcOption.getUnderlyingStock();

        Long paymentId = null;

        try {
            paymentId = transferMoney(otcOption.getBuyerId(), otcOption.getSellerId(), totalAmount);

            portfolioService.transferStockOwnership(
                    otcOption.getSellerId(),
                    otcOption.getBuyerId(),
                    stock,
                    otcOption.getAmount(),
                    offer.getPricePerStock()
            );
        } catch (Exception ex) {
            if (paymentId != null) {
                try {
                    bankClient.rejectPayment(paymentId);
                } catch (Exception rollbackEx) {
                    throw new OtcRollbackFailedException("Rollback failed after OTC execution error: " + rollbackEx.getMessage());
                }
            }
            throw new OtcExecutionFailedException("OTC execution failed: " + ex.getMessage());
        }

        offer.setStatus(OtcOfferStatus.EXERCISED);
        otcOption.setUsed(true);
        otcOptionRepository.save(otcOption);
    }

    private Long transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
        String senderAccount = bankClient.getAccountNumberByClientId(fromUserId).getBody();
        String receiverAccount = bankClient.getAccountNumberByClientId(toUserId).getBody();

        ExecutePaymentDto dto = new ExecutePaymentDto();
        dto.setSenderAccountNumber(senderAccount);
        dto.setReceiverAccountNumber(receiverAccount);
        dto.setAmount(amount);
        dto.setPaymentCode("289");
        dto.setPurposeOfPayment("OTC Option Purchase");
        dto.setReferenceNumber("OTC-" + System.currentTimeMillis());
        dto.setClientId(fromUserId);

        ResponseEntity<PaymentDto> response = bankClient.executeSystemPayment(dto);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new SystemPaymentFailedException("Payment failed: " + response.getStatusCode());
        }

        return response.getBody().getId();
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

        if(valid == null) {
            options = otcOptionRepository.findAllByBuyerId(userId);
        } else if (valid) {
            options = otcOptionRepository.findAllValid(userId, today);
        } else {
            options = otcOptionRepository.findAllInvalid(userId, today);
        }

        return options.stream()
                .map(otcOptionMapper::toDto)
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
}
