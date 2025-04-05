package rs.raf.stock_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.ClientDto;
import rs.raf.stock_service.domain.dto.CreatePaymentDto;
import rs.raf.stock_service.domain.dto.PaymentDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.repository.OptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OtcService {

    private final OptionRepository optionRepository;
    private final PortfolioService portfolioService;
    private final BankClient bankClient;
    private final UserClient userClient;

    @Transactional
    public void exerciseOption(Long optionId, Long userId) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Option not found"));

        OtcOffer offer = option.getOffer();
        if (offer == null) throw new RuntimeException("This is not an OTC option");
        if (!offer.getBuyerId().equals(userId)) throw new RuntimeException("Only buyer can exercise this option");
        if (offer.getStatus() == OtcOfferStatus.ACCEPTED) throw new RuntimeException("Already exercised");
        if (offer.getSettlementDate().isBefore(LocalDate.now())) throw new RuntimeException("Settlement expired");

        BigDecimal totalAmount = offer.getPricePerStock().multiply(BigDecimal.valueOf(offer.getAmount()));
        Stock stock = offer.getStock();

        Long paymentId = null;

        try {
            paymentId = transferMoney(offer.getBuyerId(), offer.getSellerId(), totalAmount);
            portfolioService.transferStockOwnership(offer.getSellerId(), offer.getBuyerId(), stock, offer.getAmount());
        } catch (Exception ex) {
            if (paymentId != null) {
                try {
                    bankClient.rejectPayment(paymentId);
                } catch (Exception rollbackEx) {
                    throw new RuntimeException("Rollback failed after OTC execution error: " + rollbackEx.getMessage());
                }
            }
            throw new RuntimeException("OTC execution failed: " + ex.getMessage());
        }

        offer.setStatus(OtcOfferStatus.ACCEPTED);
        optionRepository.save(option);
    }


    private Long transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
        String senderAccount = bankClient.getAccountNumberByClientId(fromUserId).getBody();
        String receiverAccount = bankClient.getAccountNumberByClientId(toUserId).getBody();

        ClientDto toClient = userClient.getClientById(toUserId);

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setSenderAccountNumber(senderAccount);
        dto.setReceiverAccountNumber(receiverAccount);
        dto.setAmount(amount);
        dto.setPurposeOfPayment("OTC Option Purchase");
        dto.setPaymentCode("289");
        dto.setReferenceNumber("OTC-" + System.currentTimeMillis());

        System.out.println("CREATE PAYMENT: " + dto);
        ResponseEntity<PaymentDto> response = bankClient.createPayment(dto);
        //500?



        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Payment failed: " + response.getStatusCode());
        }

        return response.getBody().getId(); // Vrati payment ID za eventualni rollback
    }

}
