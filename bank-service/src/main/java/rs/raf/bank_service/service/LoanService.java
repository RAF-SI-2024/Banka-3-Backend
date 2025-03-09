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


import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;

    public LoanService(LoanRepository loanRepository, LoanRequestRepository loanRequestRepository) {
        this.loanRepository = loanRepository;
        this.loanRequestRepository = loanRequestRepository;
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

