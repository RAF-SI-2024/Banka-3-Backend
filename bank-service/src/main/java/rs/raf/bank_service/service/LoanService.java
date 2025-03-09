package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.mappers.LoanMapper;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.LoanRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;

    private final CurrencyRepository currencyRepository;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, CurrencyRepository currencyRepository) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.currencyRepository = currencyRepository;
    }

    public List<LoanShortDto> getAllLoans() {
        return loanMapper.toShortDtoList(loanRepository.findAll());
    }

    public Optional<LoanDto> getLoanById(Long id) {
        return loanRepository.findById(id).map(loanMapper::toDto);
    }

    public LoanDto saveLoan(LoanDto loanDto) {
        Loan loan = loanMapper.toEntity(loanDto);
        loan.setCurrency(currencyRepository.findByCode(loanDto.getCurrencyCode()).orElseThrow(() -> new CurrencyNotFoundException(loanDto.getCurrencyCode())));
        return loanMapper.toDto(loanRepository.save(loan));
    }

    public LoanDto approveLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan with ID " + id + " not found"));

        loan.setStatus(LoanStatus.APPROVED);
        loanRepository.save(loan);

        return loanMapper.toDto(loan);
    }

    public LoanDto rejectLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan with ID " + id + " not found"));

        loan.setStatus(LoanStatus.REJECTED);
        loanRepository.save(loan);

        return loanMapper.toDto(loan);
    }
}