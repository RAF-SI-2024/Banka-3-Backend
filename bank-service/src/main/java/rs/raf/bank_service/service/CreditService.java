package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.CreditDetailedDTO;
import rs.raf.bank_service.domain.dto.CreditShortDTO;
import rs.raf.bank_service.domain.entity.Credit;
import rs.raf.bank_service.repository.CreditRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CreditService {
    private final CreditRepository creditRepository;

    public CreditService(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    public List<CreditShortDTO> getCreditsByAccountNumber(String accountNumber) {
        return creditRepository.findByAccountNumber(accountNumber)
                .stream()
                .sorted(Comparator.comparing(Credit::getAmount).reversed())
                .map(credit -> new CreditShortDTO(
                        credit.getId(),
                        credit.getAccountNumber(),
                        credit.getAmount(),
                        credit.getCreditType()
                ))
                .collect(Collectors.toList());
    }

    public Optional<CreditDetailedDTO> getCreditById(Long id) {
        return creditRepository.findById(id)
                .map(credit -> new CreditDetailedDTO(
                        credit.getId(),
                        credit.getAccountNumber(),
                        credit.getCreditType(),
                        credit.getAmount(),
                        credit.getRepaymentPeriodMonths(),
                        credit.getInterestRate(),
                        credit.getContractDate(),
                        credit.getDueDate(),
                        credit.getInstallmentAmount(),
                        credit.getNextInstallmentDate(),
                        credit.getRemainingBalance(),
                        credit.getCurrency().getName()
                ));
    }

    public Credit createCredit(Credit credit) {
        return creditRepository.save(credit);
    }
}