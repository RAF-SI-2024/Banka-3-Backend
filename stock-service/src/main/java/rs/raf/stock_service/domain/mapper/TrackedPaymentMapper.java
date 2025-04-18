package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.TrackedPaymentDto;
import rs.raf.stock_service.domain.dto.TransactionDto;
import rs.raf.stock_service.domain.entity.TrackedPayment;
import rs.raf.stock_service.domain.entity.Transaction;

@Component
public class TrackedPaymentMapper {
    public static TrackedPaymentDto toDto(TrackedPayment trackedPayment) {
        if (trackedPayment == null) return null;
        return new TrackedPaymentDto(
                trackedPayment.getId(),
                trackedPayment.getStatus()
        );
    }
}
