package rs.raf.bank_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.specification.LoanInterestRateCalculator;

@Component
public class LoanMapper {
    public LoanDto toDto(Loan loan) {
        return LoanDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .type(loan.getType())
                .amount(loan.getAmount())
                .repaymentPeriod(loan.getRepaymentPeriod())
                .nominalInterestRate(loan.getNominalInterestRate())
                .effectiveInterestRate(loan.getEffectiveInterestRate())
                .startDate(loan.getStartDate())
                .dueDate(loan.getDueDate())
                .nextInstallmentAmount(loan.getNextInstallmentAmount())
                .nextInstallmentDate(loan.getNextInstallmentDate())
                .remainingDebt(loan.getRemainingDebt())
                .currencyCode(loan.getCurrency().getCode())
                .status(loan.getStatus())
                .build();
    }

    public LoanShortDto toShortDto(Loan loan) {
        return LoanShortDto.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .type(loan.getType())
                .amount(loan.getAmount())
                .build();
    }

    public Loan toEntity(LoanDto loanDto) {
        Loan loan = new Loan();
        loan.setLoanNumber(loanDto.getLoanNumber());
        loan.setType(loanDto.getType());
        loan.setAmount(loanDto.getAmount());
        loan.setRepaymentPeriod(loanDto.getRepaymentPeriod());
        loan.setNominalInterestRate(loanDto.getNominalInterestRate());
        loan.setEffectiveInterestRate(loanDto.getEffectiveInterestRate());
        loan.setStartDate(loanDto.getStartDate());
        loan.setDueDate(loanDto.getDueDate());
        loan.setNextInstallmentAmount(loanDto.getNextInstallmentAmount());
        loan.setNextInstallmentDate(loanDto.getNextInstallmentDate());
        loan.setRemainingDebt(loanDto.getRemainingDebt());
        loan.setStatus(loanDto.getStatus());
        return loan;
    }

    public LoanDto toDtoPreview(LoanRequest loanRequest) {
        return LoanDto.builder()
                .loanNumber("N/A")
                .type(loanRequest.getType())
                .amount(loanRequest.getAmount())
                .repaymentPeriod(loanRequest.getRepaymentPeriod())
                .nominalInterestRate(LoanInterestRateCalculator.calculateNominalRate(loanRequest))
                .effectiveInterestRate(LoanInterestRateCalculator.calculateEffectiveRate(loanRequest))
                .startDate(null)
                .dueDate(null)
                .nextInstallmentAmount(null)
                .nextInstallmentDate(null)
                .remainingDebt(null)
                .currencyCode(loanRequest.getCurrency().getCode())
                .status(LoanStatus.APPROVED)
                .build();
    }
}
