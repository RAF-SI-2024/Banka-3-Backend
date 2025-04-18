package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.TrackedPaymentDto;
import rs.raf.stock_service.domain.entity.TrackedPayment;
import rs.raf.stock_service.domain.enums.TrackedPaymentStatus;
import rs.raf.stock_service.domain.enums.TrackedPaymentType;
import rs.raf.stock_service.domain.mapper.TrackedPaymentMapper;
import rs.raf.stock_service.exceptions.TrackedPaymentNotFoundException;
import rs.raf.stock_service.repository.TrackedPaymentRepository;


@Service
@Slf4j
@AllArgsConstructor
public class TrackedPaymentNotificationService {
    private final TrackedPaymentRepository trackedPaymentRepository;
    private final OtcService otcService;

    public void markAsSuccess(Long id) {
        trackedPaymentRepository.findById(id).ifPresent(trackedPayment -> {
            trackedPayment.setStatus(TrackedPaymentStatus.SUCCESS);
            trackedPaymentRepository.save(trackedPayment);

            log.info("Handling successful payment with tracked payment id {}", id);

            switch (trackedPayment.getType()) {
                case OTC_EXERCISE -> otcService.handleExerciseSuccessfulPayment(id);
                case OTC_CREATE_OPTION -> {}
                case ORDER_ALL_OR_NONE -> {}
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
                case OTC_CREATE_OPTION -> {}
                case ORDER_ALL_OR_NONE -> {}
            }
        });
    }
}
