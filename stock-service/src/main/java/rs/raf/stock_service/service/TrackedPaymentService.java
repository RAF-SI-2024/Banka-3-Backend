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
public class TrackedPaymentService {
    private final TrackedPaymentRepository trackedPaymentRepository;

    public TrackedPayment createTrackedPayment(Long entityId, TrackedPaymentType type) {
        TrackedPayment trackedPayment = new TrackedPayment();
        trackedPayment.setTrackedEntityId(entityId);
        trackedPayment.setType(type);
        trackedPayment.setStatus(TrackedPaymentStatus.PENDING);
        return trackedPaymentRepository.save(trackedPayment);
    }

    public TrackedPayment getTrackedPayment(Long id) {
        return trackedPaymentRepository.findById(id).orElseThrow(TrackedPaymentNotFoundException::new);
    }

    public TrackedPaymentDto getTrackedPaymentStatus(Long id) {
        TrackedPayment trackedPayment = getTrackedPayment(id);
        return TrackedPaymentMapper.toDto(trackedPayment);
    }

}
