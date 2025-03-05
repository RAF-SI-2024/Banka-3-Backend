package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.CreditDetailedDto;
import rs.raf.bank_service.domain.dto.CreditShortDto;
import rs.raf.bank_service.domain.entity.Credit;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.repository.CreditRepository;
import rs.raf.bank_service.repository.CurrencyRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CreditService {
    private final CreditRepository creditRepository;
    private final CurrencyRepository currencyRepository;

    public CreditService(CreditRepository creditRepository, CurrencyRepository currencyRepository) {
        this.creditRepository = creditRepository;
        this.currencyRepository = currencyRepository;
    }

    public List<CreditShortDto> getCreditsByAccountNumber(String accountNumber) {
        return creditRepository.findByAccountNumber(accountNumber)
                .stream()
                .sorted(Comparator.comparing(Credit::getAmount).reversed())
                .map(credit -> new CreditShortDto(
                        credit.getAccountNumber(),
                        credit.getAmount(),
                        credit.getCreditType()
                ))
                .collect(Collectors.toList());
    }

    public Optional<CreditDetailedDto> getCreditById(Long id) {
        return creditRepository.findById(id)
                .map(credit -> new CreditDetailedDto(
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
                        credit.getCurrency().getCode()
                ));
    }

    public CreditDetailedDto createCredit(CreditDetailedDto creditDetailedDTO) {
        Credit credit = new Credit();
        credit.setCreditType(creditDetailedDTO.getCreditType());
        credit.setAmount(creditDetailedDTO.getAmount());
        Currency currency = currencyRepository.findByCode(creditDetailedDTO.getCurrency())
                .orElseThrow(() -> new RuntimeException("Currency not found: " + creditDetailedDTO.getCurrency()));
        credit.setCurrency(currency);
        credit.setDueDate(creditDetailedDTO.getDueDate());
        credit.setContractDate(creditDetailedDTO.getContractDate());
        credit.setAccountNumber(creditDetailedDTO.getAccountNumber());
        credit.setInstallmentAmount(creditDetailedDTO.getInstallmentAmount());
        credit.setInterestRate(creditDetailedDTO.getInterestRate());
        credit.setContractDate(creditDetailedDTO.getContractDate());
        credit.setNextInstallmentDate(creditDetailedDTO.getNextInstallmentDate());
        credit.setRemainingBalance(creditDetailedDTO.getRemainingBalance());
        credit.setRepaymentPeriodMonths(creditDetailedDTO.getRepaymentPeriodMonths());
        creditRepository.save(credit);
        return creditDetailedDTO;
    }
}