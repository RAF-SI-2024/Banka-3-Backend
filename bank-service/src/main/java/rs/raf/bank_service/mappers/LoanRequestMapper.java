package rs.raf.bank_service.mappers;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.LoanRequest;

import java.util.List;
import java.util.stream.Collectors;

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
                .build();
    }

    public List<LoanRequestDto> toDtoList(List<LoanRequest> loanRequests) {
        return loanRequests.stream().map(this::toDto).collect(Collectors.toList());
    }

    public LoanRequest toEntity(LoanRequestDto loanRequestDTO) {
        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setAmount(loanRequestDTO.getAmount());
        loanRequest.setPurpose(loanRequestDTO.getPurpose());
        loanRequest.setMonthlyIncome(loanRequestDTO.getMonthlyIncome());
        loanRequest.setEmploymentDuration(loanRequestDTO.getEmploymentDuration());
        loanRequest.setRepaymentPeriod(loanRequestDTO.getRepaymentPeriod());
        loanRequest.setContactPhone(loanRequestDTO.getContactPhone());
        loanRequest.setType(loanRequestDTO.getType());
        loanRequest.setEmploymentStatus(loanRequestDTO.getEmploymentStatus());
        return loanRequest;
    }
}
