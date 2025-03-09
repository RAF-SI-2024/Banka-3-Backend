package rs.raf.bank_service.mappers;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.entity.Loan;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoanMapper {
    public LoanDto toDto(Loan loan) {
        return LoanDto.builder()
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

    public List<LoanDto> toDtoList(List<Loan> loans) {
        return loans.stream().map(this::toDto).collect(Collectors.toList());
    }

    public LoanShortDto toShortDto(Loan loan) {
        return LoanShortDto.builder().loanNumber(loan.getLoanNumber()).type(loan.getType()).amount(loan.getAmount()).build();
    }

    public List<LoanShortDto> toShortDtoList(List<Loan> loans) {
        return loans.stream().map(this::toShortDto).collect(Collectors.toList());
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

}