package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.CreditTransactionDto;
import rs.raf.bank_service.repository.CreditTransactionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreditTransactionService {
    private final CreditTransactionRepository transactionRepository;

    public CreditTransactionService(CreditTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<CreditTransactionDto> getTransactionsByCreditId(Long creditId) {
        return transactionRepository.findByCreditId(creditId).stream().map(creditTransaction -> new CreditTransactionDto(
                creditTransaction.getCredit(), creditTransaction.getTransactionDate(), creditTransaction.getAmount(), creditTransaction.isPaid()
        )).collect(Collectors.toList());
    }
}