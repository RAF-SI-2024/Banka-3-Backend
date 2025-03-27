package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.domain.dto.CreateLoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.domain.mapper.LoanMapper;
import rs.raf.bank_service.domain.mapper.LoanRequestMapper;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.BankAccountNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.LoanRequestNotFoundException;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.specification.LoanInterestRateCalculator;
import rs.raf.bank_service.specification.LoanRateCalculator;
import rs.raf.bank_service.specification.LoanRequestSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

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

    public LoanDto returnLoanDto(Long id) {
        LoanRequest loanRequest = loanRequestRepository.findByIdAndStatus(id, LoanRequestStatus.PENDING)
                .orElseThrow(() -> new LoanRequestNotFoundException());

        return loanMapper.toDtoPreview(loanRequest);
    }


    public LoanRequestDto saveLoanRequest(CreateLoanRequestDto createLoanRequestDTO, String authHeader) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        LoanRequest loanRequest = loanRequestMapper.createRequestToEntity(createLoanRequestDTO);

        loanRequest.setCurrency(currencyRepository.findByCode(createLoanRequestDTO.getCurrencyCode()).orElseThrow(() -> new CurrencyNotFoundException(createLoanRequestDTO.getCurrencyCode())));
        loanRequest.setAccount(accountRepository.findByAccountNumberAndClientId(createLoanRequestDTO.getAccountNumber(), clientId).orElseThrow(AccountNotFoundException::new));

        loanRequest.setStatus(LoanRequestStatus.PENDING);
        loanRequest = loanRequestRepository.save(loanRequest);
        return loanRequestMapper.toDto(loanRequest);
    }

    @Transactional
    public LoanDto approveLoan(Long id) {
        LoanRequest loanRequest = loanRequestRepository.findByIdAndStatus(id, LoanRequestStatus.PENDING)
                .orElseThrow(LoanRequestNotFoundException::new);

        loanRequest.setStatus(LoanRequestStatus.APPROVED);


        CompanyAccount bankAccount = accountRepository
                .findFirstByCurrencyAndCompanyId(loanRequest.getCurrency(), 1L)
                .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + loanRequest.getCurrency().getCode()));


        bankAccount.setBalance(bankAccount.getBalance().subtract(loanRequest.getAmount()));
        bankAccount.setAvailableBalance(bankAccount.getAvailableBalance().subtract(loanRequest.getAmount()));
        accountRepository.save(bankAccount);

        Account userAccount = loanRequest.getAccount();
        userAccount.setBalance(userAccount.getBalance().add(loanRequest.getAmount()));
        userAccount.setAvailableBalance(userAccount.getAvailableBalance().add(loanRequest.getAmount()));
        accountRepository.save(userAccount);

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
                .account(userAccount)
                .build();

        Installment installment = new Installment(
                loan,
                loan.getNextInstallmentAmount(),
                loan.getEffectiveInterestRate(),
                loan.getNextInstallmentDate(),
                InstallmentStatus.UNPAID
        );

        loan.setInstallments(new ArrayList<>());
        loan.getInstallments().add(installment);

        loanRepository.save(loan);
        installmentRepository.save(installment);

        return loanMapper.toDto(loan);
    }

    public LoanRequestDto rejectLoan(Long id) {
        LoanRequest loan = loanRequestRepository.findByIdAndStatus(id, LoanRequestStatus.PENDING).orElseThrow(LoanRequestNotFoundException::new);

        loan.setStatus(LoanRequestStatus.REJECTED);
        loanRequestRepository.save(loan);

        return loanRequestMapper.toDto(loan);
    }

    public Page<LoanRequestDto> getClientLoanRequests(String authHeader, Pageable pageable) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        List<Account> accounts = accountRepository.findByClientId(clientId);

        return loanRequestRepository.findByAccountIn(accounts, pageable).map(loanRequestMapper::toDto);
    }

    public Page<LoanRequestDto> getAllLoanRequests(LoanType type, String accountNumber, Pageable pageable) {
        Specification<LoanRequest> spec = LoanRequestSpecification.filterBy(type, accountNumber);

        return loanRequestRepository.findAll(spec, pageable).map(loanRequestMapper::toDto);
    }
}