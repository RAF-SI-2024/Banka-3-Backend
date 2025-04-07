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
import rs.raf.stock_service.domain.entity.OtcOffer;
import rs.raf.stock_service.domain.entity.OtcOption;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.repository.OptionRepository;
import rs.raf.stock_service.repository.OtcOptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OtcService {

    private final OptionRepository optionRepository;
    private final PortfolioService portfolioService;
    private final BankClient bankClient;
    private final UserClient userClient;
    private final OtcOptionRepository otcOptionRepository;

    @Transactional
    public void exerciseOption(Long otcOptionId, Long userId) {
        OtcOption otcOption = otcOptionRepository.findById(otcOptionId)
                .orElseThrow(() -> new RuntimeException("OTC Option not found"));

        OtcOffer offer = otcOption.getOtcOffer();
        if (offer == null) throw new RuntimeException("This is not an OTC option");
        if (!otcOption.getBuyerId().equals(userId)) throw new RuntimeException("Only buyer can exercise this option");
        if (offer.getStatus() == OtcOfferStatus.EXERCISED) throw new RuntimeException("Option already exercised");
        if (otcOption.getSettlementDate().isBefore(LocalDate.now())) throw new RuntimeException("Settlement expired");

        BigDecimal totalAmount = offer.getPricePerStock().multiply(BigDecimal.valueOf(otcOption.getAmount()));
        Stock stock = otcOption.getUnderlyingStock();

        Long paymentId = null;

        try {
            transferMoney(otcOption.getBuyerId(), otcOption.getSellerId(), totalAmount);

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
                    throw new RuntimeException("Rollback failed after OTC execution error: " + rollbackEx.getMessage());
                }
            }
            throw new RuntimeException("OTC execution failed: " + ex.getMessage());
        }

        offer.setStatus(OtcOfferStatus.EXERCISED);
        otcOption.setUsed(true);
        otcOptionRepository.save(otcOption);
    }

    private void transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
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

        bankClient.executeSystemPayment(dto);
    }
}
