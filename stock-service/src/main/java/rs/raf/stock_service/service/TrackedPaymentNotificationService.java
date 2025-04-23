package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.enums.TrackedPaymentStatus;
import rs.raf.stock_service.repository.TrackedPaymentRepository;


@Service
@Slf4j
@AllArgsConstructor
public class TrackedPaymentNotificationService {
    private final TrackedPaymentRepository trackedPaymentRepository;
    private final OtcService otcService;
    private final TaxService taxService;
    private final OrderService orderService;

    public void markAsSuccess(Long id) {
        trackedPaymentRepository.findById(id).ifPresent(trackedPayment -> {
            trackedPayment.setStatus(TrackedPaymentStatus.SUCCESS);
            trackedPaymentRepository.save(trackedPayment);

            log.info("Handling successful payment with tracked payment id {}", id);

            switch (trackedPayment.getType()) {
                case OTC_EXERCISE -> otcService.handleExerciseSuccessfulPayment(id);
                case OTC_CREATE_OPTION -> otcService.handleAcceptSuccessfulPayment(id);
                case ORDER_TRANSACTION -> orderService.handleTransactionSuccessfulPayment(id);
                case TAX_PAYMENT -> taxService.handleSuccessfulTaxPayment(id);
                case ORDER_COMMISSION -> orderService.handleCommissionSuccessfulPayment(id);
            }
        });

    }

    public void markAsFail(Long id) {
        trackedPaymentRepository.findById(id).ifPresent(trackedPayment -> {
            trackedPayment.setStatus(TrackedPaymentStatus.FAIL);
            trackedPaymentRepository.save(trackedPayment);

            log.info("Handling failed payment with tracked payment id {}", id);

            switch (trackedPayment.getType()) {
                case OTC_EXERCISE -> {}
                case OTC_CREATE_OPTION -> otcService.handleAcceptFailedPayment(id);
                case ORDER_TRANSACTION -> orderService.handleTransactionFailedPayment(id);
                case TAX_PAYMENT -> taxService.handleFailedTaxPayment(id);
            }
        });
    }
}
