package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.domain.mapper.InstallmentMapper;
import rs.raf.bank_service.domain.mapper.LoanMapper;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.specification.LoanRateCalculator;
import rs.raf.bank_service.specification.LoanSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanMapper loanMapper;
    private final AccountRepository accountRepository;
    private final UserClient userClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private final JwtTokenUtil jwtTokenUtil;
    private final InstallmentRepository installmentRepository;
    private final InstallmentMapper installmentMapper;
    private final TransactionQueueService transactionQueueService;

    public List<InstallmentDto> getLoanInstallments(Long loanId) {
        return installmentRepository.findByLoanId(loanId).stream().map(installmentMapper::toDto).collect(Collectors.toList());
    }

    public Page<LoanShortDto> getClientLoans(String authHeader, Pageable pageable) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);
        List<Account> accounts = accountRepository.findByClientId(clientId);

        return loanRepository.findByAccountIn(accounts, pageable).map(loanMapper::toShortDto);
    }

    public Optional<LoanDto> getLoanById(Long id) {
        return loanRepository.findById(id).map(loanMapper::toDto);
    }

    public Page<LoanDto> getAllLoans(LoanType type, String accountNumber, LoanStatus status, Pageable pageable) {
        Specification<Loan> spec = LoanSpecification.filterBy(type, accountNumber, status);

        return loanRepository.findAll(spec, pageable).map(loanMapper::toDto);
    }

    public Long findLoanIdByLoanRequestId(Long loanRequestId) {
        return loanRepository.findAll()
                .stream()
                .filter(l -> l.getAccount().getAccountNumber()
                        .equals(loanRequestRepository.findById(loanRequestId).orElseThrow().getAccount().getAccountNumber()))
                .sorted(Comparator.comparing(Loan::getStartDate).reversed())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No loan found for request " + loanRequestId))
                .getId();
    }


    @Transactional
    public void payInstallment(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus().compareTo(LoanStatus.PAID_OFF) >= 0) return;

        Account account = loan.getAccount();

        if (account.getBalance().compareTo(loan.getNextInstallmentAmount()) < 0) {
            throw new RuntimeException("Insufficient funds for loan installment.");
        }

        account.setBalance(account.getBalance().subtract(loan.getNextInstallmentAmount()));
        accountRepository.save(account);

        List<Installment> installments = loan.getInstallments();
        installments.sort(Comparator.comparing(Installment::getId));

        Installment current = installments.get(installments.size() - 1);
        current.setInstallmentStatus(InstallmentStatus.PAID);
        current.setActualDueDate(LocalDate.now());
        installmentRepository.save(current);

        if (installments.size() < loan.getRepaymentPeriod()) {
            Installment next = new Installment(
                    loan,
                    LoanRateCalculator.calculateMonthlyRate(loan.getAmount(), loan.getEffectiveInterestRate(), loan.getRepaymentPeriod()),
                    loan.getEffectiveInterestRate(),
                    LocalDate.now().plusMonths(1),
                    InstallmentStatus.UNPAID
            );
            loan.getInstallments().add(next);
            loan.setNextInstallmentDate(next.getExpectedDueDate());
            installmentRepository.save(next);
        } else {
            loan.setStatus(LoanStatus.PAID_OFF);
        }

        loanRepository.save(loan);
    }

    @Scheduled(cron = "0 0 2 * * *") // svaki dan u 2h ujutru
    public void queueDueInstallments() {
        LocalDate today = LocalDate.now();

        List<Loan> loans = loanRepository.findByNextInstallmentDateAndStartDateBefore(today, today);

        for (Loan loan : loans) {
            transactionQueueService.queueTransaction("PAY_INSTALLMENT", loan.getId());
        }

        log.info("Queued {} loan installments for {}", loans.size(), today);
    }


    private void scheduleRetry(Loan loan, long delay, TimeUnit timeUnit) {
        scheduledExecutorService.schedule(() -> retryLoanPayment(loan), delay, timeUnit);
    }

    @Transactional
    public void retryLoanPayment(Loan loan) {
        Account currAccount = accountRepository.findByAccountNumber(loan.getAccount().getAccountNumber()).orElseThrow();
        if (loan.getStatus().compareTo(LoanStatus.PAID_OFF) < 0 && currAccount.getBalance().compareTo(loan.getNextInstallmentAmount()) >= 0) {
            currAccount.setBalance(currAccount.getBalance().subtract(loan.getNextInstallmentAmount()));
            accountRepository.save(currAccount);

            List<Installment> installments = loan.getInstallments();
            installments.sort(Comparator.comparing(Installment::getId));
            Installment  installment1= installments.get(installments.size()-1);
            installment1.setInstallmentStatus(InstallmentStatus.PAID);
            installment1.setActualDueDate(LocalDate.now());

            if (loan.getInstallments().size() < loan.getRepaymentPeriod()) {
                Installment newInstallment = new Installment(loan, LoanRateCalculator.calculateMonthlyRate(loan.getAmount()
                        ,loan.getEffectiveInterestRate(),
                        loan.getRepaymentPeriod()), loan.getEffectiveInterestRate(), LocalDate.now(), InstallmentStatus.UNPAID);
                loan.getInstallments().add(newInstallment);
                loan.setNextInstallmentDate(newInstallment.getExpectedDueDate());
                loan.setNextInstallmentAmount(newInstallment.getAmount());
                loanRepository.save(loan);
            } else
                loan.setStatus(LoanStatus.PAID_OFF);


        } else {
            EmailRequestDto emailRequestDto = new EmailRequestDto();
            emailRequestDto.setCode("INSUFFICIENT-FUNDS");
            Long clientId = loan.getAccount().getClientId();
            ClientDto client = userClient.getClientById(clientId);
            emailRequestDto.setDestination(client.getEmail());
            //rabbitTemplate.convertAndSend("insufficient-funds", emailRequestDto);
            loan.setNominalInterestRate(loan.getNominalInterestRate().add(new BigDecimal("0.05")));
            loan.setEffectiveInterestRate(loan.getEffectiveInterestRate().add(new BigDecimal("0.05")));
            scheduleRetry(loan, 72, TimeUnit.HOURS);
            loanRepository.save(loan);
        }
    }

}