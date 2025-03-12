package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.mappers.LoanMapper;
import rs.raf.bank_service.mappers.LoanRequestMapper;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.specification.LoanInterestRateCalculator;
import rs.raf.bank_service.specification.LoanRateCalculator;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class LoanRequestService {
    private final LoanRequestRepository loanRequestRepository;
    private final LoanRequestMapper loanRequestMapper;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final CurrencyRepository currencyRepository;
    private final InstallmentRepository installmentRepository;
    private final JwtTokenUtil jwtTokenUtil;


    public List<LoanRequestDto> getLoanRequestsByStatus(LoanRequestStatus status) {
        return loanRequestMapper.toDtoList(loanRequestRepository.findByStatus(status));
    }

    public LoanRequestDto saveLoanRequest(LoanRequestDto loanRequestDTO, String authHeader) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        LoanRequest loanRequest = loanRequestMapper.toEntity(loanRequestDTO);

        loanRequest.setCurrency(currencyRepository.findByCode(loanRequestDTO.getCurrencyCode()).orElseThrow(() -> new CurrencyNotFoundException(loanRequestDTO.getCurrencyCode())));
        loanRequest.setAccount(accountRepository.findByAccountNumberAndClientId(loanRequestDTO.getAccountNumber(), clientId).orElseThrow(AccountNotFoundException::new));

        loanRequest.setStatus(LoanRequestStatus.PENDING);
        loanRequestRepository.save(loanRequest);
        return loanRequestMapper.toDto(loanRequest);
    }

    public LoanDto approveLoan(Long id) {
        LoanRequest loanRequest = loanRequestRepository.findById(id).orElseThrow(LoanRequestNotFoundException::new);
        loanRequest.setStatus(LoanRequestStatus.APPROVED);
        Loan loan = Loan.builder()
                .loanNumber(UUID.randomUUID().toString())
                .type(loanRequest.getType())
                .amount(loanRequest.getAmount())
                .repaymentPeriod(loanRequest.getRepaymentPeriod())
                .nominalInterestRate(LoanInterestRateCalculator.calculateNominalRate(loanRequest))
                .effectiveInterestRate(LoanInterestRateCalculator.calculateEffectiveRate(loanRequest))
                .startDate(LocalDate.now())
                .dueDate(LocalDate.now().plusMonths(loanRequest.getRepaymentPeriod()))
                .nextInstallmentAmount(LoanRateCalculator.calculateMonthlyRate(
                        loanRequest.getAmount(),
                        LoanInterestRateCalculator.calculateEffectiveRate(loanRequest),
                        loanRequest.getRepaymentPeriod()))
                .nextInstallmentDate(LocalDate.now().plusMonths(1))
                .remainingDebt(loanRequest.getAmount())
                .currency(loanRequest.getCurrency())
                .status(LoanStatus.APPROVED)
                .interestRateType(loanRequest.getInterestRateType())
                .account(loanRequest.getAccount())
                .build();

        Installment installment = new Installment(loan, loan.getNextInstallmentAmount(), loan.getEffectiveInterestRate(), loan.getNextInstallmentDate(), InstallmentStatus.UNPAID);
        loan.setInstallments(new ArrayList<>());
        loan.getInstallments().add(installment);
        loanRepository.save(loan);
        installmentRepository.save(installment);

        loanRepository.save(loan);
        return loanMapper.toDto(loan);
    }

    public LoanRequestDto rejectLoan(Long id) {
        LoanRequest loan = loanRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan request with ID " + id + " not found"));

        loan.setStatus(LoanRequestStatus.REJECTED);
        loanRequestRepository.save(loan);

        return loanRequestMapper.toDto(loan);
    }
}