package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.mappers.LoanRequestMapper;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.LoanRequestRepository;

import java.util.List;

@Service
public class LoanRequestService {
    private final LoanRequestRepository loanRequestRepository;
    private final LoanRequestMapper loanRequestMapper;
    private final AccountRepository accountRepository;

    private final CurrencyRepository currencyRepository;

    public LoanRequestService(LoanRequestRepository loanRequestRepository, LoanRequestMapper loanRequestMapper, CurrencyRepository currencyRepository, AccountRepository accountRepository) {
        this.loanRequestRepository = loanRequestRepository;
        this.loanRequestMapper = loanRequestMapper;
        this.currencyRepository = currencyRepository;
        this.accountRepository = accountRepository;
    }

    public List<LoanRequestDto> getLoanRequestsByStatus(LoanRequestStatus status) {
        return loanRequestMapper.toDtoList(loanRequestRepository.findByStatus(status));
    }

    public LoanRequestDto saveLoanRequest(LoanRequestDto loanRequestDTO) {
        LoanRequest loanRequest = loanRequestMapper.toEntity(loanRequestDTO);

        loanRequest.setCurrency(currencyRepository.findByCode(loanRequestDTO.getCurrencyCode()).orElseThrow(() -> new CurrencyNotFoundException(loanRequestDTO.getCurrencyCode())));
        loanRequest.setAccount(accountRepository.findByAccountNumber(loanRequestDTO.getAccountNumber()).orElseThrow(AccountNotFoundException::new));
        LoanType loanType = null;
        InterestRateType interestRateType = null;
        EmploymentStatus employmentStatus = null;
        try {
            loanType = LoanType.valueOf(loanRequestDTO.getType());
            interestRateType = InterestRateType.valueOf(loanRequestDTO.getInterestRateType());
            employmentStatus = EmploymentStatus.valueOf(loanRequestDTO.getEmploymentStatus());
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("LoanType"))
                throw new InvalidLoanTypeException();
            else if(e.getMessage().contains("InterestRateType"))
                throw new InvalidInterestRateTypeException();
            else if(e.getMessage().contains("EmploymentStatus"))
                throw new InvalidEmploymentStatusException();
        }
        loanRequest.setType(loanType);
        loanRequest.setInterestRateType(interestRateType);
        loanRequest.setEmploymentStatus(employmentStatus);
        loanRequest.setStatus(LoanRequestStatus.PENDING);
        loanRequestRepository.save(loanRequest);
        return loanRequestMapper.toDto(loanRequest);
    }
}