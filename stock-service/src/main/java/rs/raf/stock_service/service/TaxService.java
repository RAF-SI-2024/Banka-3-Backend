package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.stock_service.client.BankClient;
import rs.raf.stock_service.client.UserClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Order;
import rs.raf.stock_service.domain.entity.OtcOption;
import rs.raf.stock_service.domain.entity.TrackedPayment;
import rs.raf.stock_service.domain.enums.TaxStatus;
import rs.raf.stock_service.domain.enums.TrackedPaymentStatus;
import rs.raf.stock_service.domain.enums.TrackedPaymentType;
import rs.raf.stock_service.domain.mapper.TrackedPaymentMapper;
import rs.raf.stock_service.exceptions.InsufficientFundsException;
import rs.raf.stock_service.exceptions.OrderNotFoundException;
import rs.raf.stock_service.exceptions.OtcOptionNotFoundException;
import rs.raf.stock_service.repository.OrderRepository;
import rs.raf.stock_service.repository.TrackedPaymentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class TaxService {

    private final UserClient userClient;
    private final BankClient bankClient;
    private final PortfolioService portfolioService;
    private final OrderRepository orderRepository;
    private final TrackedPaymentService trackedPaymentService;
    private final TrackedPaymentRepository trackedPaymentRepository;
    private final TrackedPaymentMapper trackedPaymentMapper;

    public List<UserTaxDto> getTaxes(String name, String surname, String role) {
        List<UserTaxDto> userTaxDtos = userClient.getAgentsAndClients(name, surname, role);
        for (UserTaxDto userTaxDto : userTaxDtos) {
            TaxGetResponseDto taxForUser = portfolioService.getUserTaxes(userTaxDto.getId());
            userTaxDto.setUnpaidTaxThisMonth(taxForUser.getUnpaidForThisMonth());
            userTaxDto.setPaidTaxThisYear(taxForUser.getPaidForThisYear());
        }
        return userTaxDtos;
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public TrackedPaymentDto processTaxes() {
        List<Order> eligibleOrders = orderRepository.findAll().stream()
                .filter(order -> order.getTaxAmount() != null && order.getTaxStatus().equals(TaxStatus.PENDING))
                .toList();

        if (eligibleOrders.isEmpty()) {
            return null;
        }

        // Pre-validate all orders to ensure sufficient funds
        for (Order order : eligibleOrders) {
            AccountDetailsDto accountDetails = bankClient.getAccountDetails(order.getAccountNumber());
            BigDecimal taxAmount = convertTaxAmountIfNeeded(order.getTaxAmount(), accountDetails.getCurrencyCode());

            if (accountDetails.getBalance().compareTo(taxAmount) < 0) {
                throw new InsufficientFundsException(taxAmount);
            }
        }

        TrackedPayment mainTrackedPayment = trackedPaymentService.createTrackedPayment(
                null,
                TrackedPaymentType.TAX_PROCESSOR
        );

        // Process each eligible order
        for (Order order : eligibleOrders) {
            AccountDetailsDto accountDetails = bankClient.getAccountDetails(order.getAccountNumber());
            BigDecimal taxAmount = convertTaxAmountIfNeeded(order.getTaxAmount(), accountDetails.getCurrencyCode());

            TrackedPayment trackedPayment = trackedPaymentService.createTrackedPayment(
                    order.getId(),
                    mainTrackedPayment.getId(),
                    TrackedPaymentType.TAX_PAYMENT
            );

            ExecutePaymentDto executePaymentDto = new ExecutePaymentDto();
            executePaymentDto.setClientId(order.getUserId());
            executePaymentDto.setCreatePaymentDto(
                    CreatePaymentDto.builder()
                            .amount(taxAmount)
                            .senderAccountNumber(order.getAccountNumber())
                            .callbackId(trackedPayment.getId())
                            .build()
            );
            bankClient.handleTax(executePaymentDto);
        }
        return TrackedPaymentMapper.toDto(mainTrackedPayment);
    }

    private BigDecimal convertTaxAmountIfNeeded(BigDecimal taxAmount, String currencyCode) {
        if (!currencyCode.equals("USD")) {
            ConvertDto convertDto = new ConvertDto("USD", currencyCode, taxAmount);
            return bankClient.convert(convertDto);
        }
        return taxAmount;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handleSuccessfulTaxPayment(Long id) {
        TrackedPayment thisTaxPayment = trackedPaymentService.getTrackedPayment(id);

        thisTaxPayment.setStatus(TrackedPaymentStatus.SUCCESS);
        trackedPaymentRepository.save(thisTaxPayment);

        Long orderId = thisTaxPayment.getTrackedEntityId();

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setTaxStatus(TaxStatus.PAID);
        orderRepository.save(order);

        List<TrackedPayment> taxPayments = trackedPaymentService.getTrackedPaymentsForTaxProcessor(thisTaxPayment.getSecondaryTrackedEntityId());

        for (TrackedPayment taxPayment : taxPayments) {
            TrackedPaymentStatus status = taxPayment.getStatus();
            if (status.equals(TrackedPaymentStatus.FAIL)) return;

            if (status.equals(TrackedPaymentStatus.PENDING) && !thisTaxPayment.getId().equals(taxPayment.getId())) return;
        }

        TrackedPayment taxProcessor = trackedPaymentService.getTrackedPayment(thisTaxPayment.getSecondaryTrackedEntityId());
        taxProcessor.setStatus(TrackedPaymentStatus.SUCCESS);
        trackedPaymentRepository.save(taxProcessor);
    }

    public void handleFailedTaxPayment(Long id) {
        TrackedPayment trackedPayment = trackedPaymentService.getTrackedPayment(id);

        Long taxProcessEntityId = trackedPayment.getTrackedEntityId();
        TrackedPayment taxProcessEntity = trackedPaymentService.getTrackedPayment(taxProcessEntityId);

        taxProcessEntity.setStatus(TrackedPaymentStatus.FAIL);

        trackedPaymentRepository.save(taxProcessEntity);

    }


}
