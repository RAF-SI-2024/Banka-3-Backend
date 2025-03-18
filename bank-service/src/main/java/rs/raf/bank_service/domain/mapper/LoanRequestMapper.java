package rs.raf.bank_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.CreateLoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.LoanRequest;

@Component
public class LoanRequestMapper {
    public LoanRequestDto toDto(LoanRequest loanRequest) {
        return LoanRequestDto.builder()
                .type(loanRequest.getType())
                .amount(loanRequest.getAmount())
                .purpose(loanRequest.getPurpose())
                .monthlyIncome(loanRequest.getMonthlyIncome())
                .employmentStatus(loanRequest.getEmploymentStatus())
                .employmentDuration(loanRequest.getEmploymentDuration())
                .repaymentPeriod(loanRequest.getRepaymentPeriod())
                .contactPhone(loanRequest.getContactPhone())
                .currencyCode(loanRequest.getCurrency().getCode())
                .accountNumber(loanRequest.getAccount().getAccountNumber())
                .interestRateType(loanRequest.getInterestRateType())
                .createdAt(loanRequest.getCreatedAt())
                .status(loanRequest.getStatus())
                .build();
    }

    public LoanRequest createRequestToEntity(CreateLoanRequestDto createLoanRequestDTO) {
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAmount(createLoanRequestDTO.getAmount());
        loanRequest.setPurpose(createLoanRequestDTO.getPurpose());
        loanRequest.setMonthlyIncome(createLoanRequestDTO.getMonthlyIncome());
        loanRequest.setEmploymentDuration(createLoanRequestDTO.getEmploymentDuration());
        loanRequest.setRepaymentPeriod(createLoanRequestDTO.getRepaymentPeriod());
        loanRequest.setContactPhone(createLoanRequestDTO.getContactPhone());
        loanRequest.setType(createLoanRequestDTO.getType());
        loanRequest.setEmploymentStatus(createLoanRequestDTO.getEmploymentStatus());
        loanRequest.setInterestRateType(createLoanRequestDTO.getInterestRateType());
        return loanRequest;
    }
}
