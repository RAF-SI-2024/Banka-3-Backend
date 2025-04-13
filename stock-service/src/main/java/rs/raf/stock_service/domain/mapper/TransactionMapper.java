package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.TransactionDto;
import rs.raf.stock_service.domain.entity.Transaction;

@Component
public class TransactionMapper {

    public static TransactionDto toDto(Transaction transaction) {
        if (transaction == null) return null;
        return new TransactionDto(
                transaction.getId(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getTotalPrice(),
                transaction.getTimestamp()
        );
    }
}
