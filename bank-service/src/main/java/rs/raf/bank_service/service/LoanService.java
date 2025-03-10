package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.exceptions.LoanNotFoundException;
import rs.raf.bank_service.repository.LoanRepository;
import rs.raf.bank_service.repository.LoanRequestRepository;
import rs.raf.bank_service.specification.LoanInterestRateCalculator;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.mappers.LoanMapper;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanMapper loanMapper;

    private final CurrencyRepository currencyRepository;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, CurrencyRepository currencyRepository, LoanRequestRepository loanRequestRepository) {
        this.loanRepository = loanRepository;
        this.loanRequestRepository = loanRequestRepository;
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

    public Loan approveLoan(Long loanRequestId) {
        LoanRequest request = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new LoanNotFoundException());

        Loan loan = Loan.builder()
                .loanNumber("LN-" + loanRequestId)
                .type(request.getType())
                .amount(request.getAmount())
                .repaymentPeriod(request.getRepaymentPeriod())
                .nominalInterestRate(LoanInterestRateCalculator.calculateNominalRate(request))
                .effectiveInterestRate(LoanInterestRateCalculator.calculateEffectiveRate(request))
                .startDate(LocalDate.now())
                .dueDate(LocalDate.now().plusMonths(request.getRepaymentPeriod()))
                .remainingDebt(request.getAmount())
                .status(LoanStatus.APPROVED)
                .account(request.getAccount())
                .currency(request.getCurrency())
                .interestRateType(InterestRateType.FIXED)
                .build();

        request.setStatus(LoanRequestStatus.APPROVED);
        loanRequestRepository.save(request);
        return loanRepository.save(loan);
    }

    public void rejectLoan(Long loanRequestId) {
        LoanRequest request = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new LoanNotFoundException());

        request.setStatus(LoanRequestStatus.REJECTED);
        loanRequestRepository.save(request);
    }
}