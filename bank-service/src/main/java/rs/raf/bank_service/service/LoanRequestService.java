package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.mappers.LoanRequestMapper;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.LoanRequestRepository;

import java.util.List;

@Service
public class LoanRequestService {
    private final LoanRequestRepository loanRequestRepository;
    private final LoanRequestMapper loanRequestMapper;

    private final CurrencyRepository currencyRepository;

    public LoanRequestService(LoanRequestRepository loanRequestRepository, LoanRequestMapper loanRequestMapper, CurrencyRepository currencyRepository) {
        this.loanRequestRepository = loanRequestRepository;
        this.loanRequestMapper = loanRequestMapper;
        this.currencyRepository = currencyRepository;
    }

    public List<LoanRequestDto> getLoanRequestsByStatus(LoanRequestStatus status) {
        return loanRequestMapper.toDtoList(loanRequestRepository.findByStatus(status));
    }

    public LoanRequestDto saveLoanRequest(LoanRequestDto loanRequestDTO) {
        LoanRequest loanRequest = loanRequestMapper.toEntity(loanRequestDTO);
        loanRequest.setCurrency(currencyRepository.findByCode(loanRequestDTO.getCurrencyCode()).orElseThrow(() -> new CurrencyNotFoundException(loanRequestDTO.getCurrencyCode())));
        return loanRequestMapper.toDto(loanRequestRepository.save(loanRequest));
    }
}