package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.domain.mapper.InstallmentMapper;
import rs.raf.bank_service.domain.mapper.LoanMapper;
import rs.raf.bank_service.exceptions.BankAccountNotFoundException;
import rs.raf.bank_service.exceptions.InsufficientFundsException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.InstallmentRepository;
import rs.raf.bank_service.repository.LoanRepository;
import rs.raf.bank_service.repository.LoanRequestRepository;
import rs.raf.bank_service.specification.LoanRateCalculator;
import rs.raf.bank_service.specification.LoanSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
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
            throw new InsufficientFundsException(account.getBalance(), loan.getNextInstallmentAmount());
        }

        BigDecimal amount = loan.getNextInstallmentAmount();

        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        accountRepository.save(account);

        //azurira stanje banke
        CompanyAccount bankAccount = accountRepository
                .findFirstByCurrencyAndCompanyId(account.getCurrency(), 1L)
                .orElseThrow(() -> new BankAccountNotFoundException("Bank account not found for currency: " + account.getCurrency().getCode()));

        bankAccount.setBalance(bankAccount.getBalance().add(amount));
        bankAccount.setAvailableBalance(bankAccount.getBalance());
        accountRepository.save(bankAccount);

        // azurira remainingDebt
        BigDecimal updatedDebt = loan.getRemainingDebt().subtract(amount);
        loan.setRemainingDebt(updatedDebt.max(BigDecimal.ZERO));

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

    @Scheduled(cron = "*/15 * * * * *")
    //@Scheduled(cron = "0 0 2 * * *") ovako je svaki dan u 02:00h ali zbog testiranja koristimo ovo gore koje radi na 15sec
    public void queueDueInstallments() {
        LocalDate today = LocalDate.now();


        List<Loan> loans = loanRepository.findByNextInstallmentDateAndStartDateBefore(today, today);

        for (Loan loan : loans) {
            transactionQueueService.queueTransaction(TransactionType.PAY_INSTALLMENT, loan.getId());
        }

        log.info("Queued {} loan installments for {}", loans.size(), today);
    }


    private void scheduleRetry(Loan loan, long delay, TimeUnit timeUnit) {
        scheduledExecutorService.schedule(() -> retryLoanPayment(loan), delay, timeUnit);
    }

    @Transactional
    public void retryLoanPayment(Loan loan) {
        Account currAccount = accountRepository.findByAccountNumber(loan.getAccount().getAccountNumber()).orElseThrow();

        if (loan.getStatus().compareTo(LoanStatus.PAID_OFF) < 0 &&
                currAccount.getBalance().compareTo(loan.getNextInstallmentAmount()) >= 0) {

            BigDecimal amount = loan.getNextInstallmentAmount();

            currAccount.setBalance(currAccount.getBalance().subtract(amount));
            currAccount.setAvailableBalance(currAccount.getAvailableBalance().subtract(amount));
            accountRepository.save(currAccount);

            // Ažuriraj remainingDebt
            BigDecimal updatedDebt = loan.getRemainingDebt().subtract(amount);
            loan.setRemainingDebt(updatedDebt.max(BigDecimal.ZERO));

            List<Installment> installments = loan.getInstallments();
            installments.sort(Comparator.comparing(Installment::getId));
            Installment installment1 = installments.get(installments.size() - 1);
            installment1.setInstallmentStatus(InstallmentStatus.PAID);
            installment1.setActualDueDate(LocalDate.now());
            installmentRepository.save(installment1);

            if (loan.getInstallments().size() < loan.getRepaymentPeriod()) {
                Installment newInstallment = new Installment(
                        loan,
                        LoanRateCalculator.calculateMonthlyRate(loan.getAmount(), loan.getEffectiveInterestRate(), loan.getRepaymentPeriod()),
                        loan.getEffectiveInterestRate(),
                        LocalDate.now().plusMonths(1),
                        InstallmentStatus.UNPAID
                );
                loan.getInstallments().add(newInstallment);
                loan.setNextInstallmentDate(newInstallment.getExpectedDueDate());
                loan.setNextInstallmentAmount(newInstallment.getAmount());
                installmentRepository.save(newInstallment);
            } else {
                loan.setStatus(LoanStatus.PAID_OFF);
            }

            loanRepository.save(loan);

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

    @Scheduled(cron = "0 0 3 1 * *") //prvog u mesecu
    //@Scheduled(fixedRate = 15000) //za test
    @Transactional
    public void updateVariableInterestRates() {
        List<Loan> variableLoans = loanRepository.findAll().stream()
                .filter(loan -> loan.getInterestRateType().equals(InterestRateType.VARIABLE))
                .collect(Collectors.toList());

        for (Loan loan : variableLoans) {
            BigDecimal osnovica = getOsnovica(loan.getAmount());
            BigDecimal pomeraj = getRandomPomeraj();
            BigDecimal marza = getMarza(loan.getType());

            BigDecimal novaEfektivna = osnovica.add(pomeraj).add(marza.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP));

            loan.setNominalInterestRate(osnovica);
            loan.setEffectiveInterestRate(novaEfektivna);

            BigDecimal novaRata = LoanRateCalculator.calculateMonthlyRate(
                    loan.getAmount(),
                    novaEfektivna,
                    loan.getRepaymentPeriod()
            );

            loan.setNextInstallmentAmount(novaRata);
            loanRepository.save(loan);
        }

        log.info("Updated variable interest rates for {} loans.", variableLoans.size());
    }
    private BigDecimal getOsnovica(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("500000")) <= 0) return new BigDecimal("6.25");
        if (amount.compareTo(new BigDecimal("1000000")) <= 0) return new BigDecimal("6.00");
        if (amount.compareTo(new BigDecimal("2000000")) <= 0) return new BigDecimal("5.75");
        if (amount.compareTo(new BigDecimal("5000000")) <= 0) return new BigDecimal("5.50");
        if (amount.compareTo(new BigDecimal("10000000")) <= 0) return new BigDecimal("5.25");
        if (amount.compareTo(new BigDecimal("20000000")) <= 0) return new BigDecimal("5.00");
        return new BigDecimal("4.75");
    }

    private BigDecimal getMarza(LoanType type) {
        switch (type) {
            case CASH: return new BigDecimal("1.75");
            case MORTGAGE: return new BigDecimal("1.50");
            case AUTO: return new BigDecimal("1.25");
            case REFINANCING: return new BigDecimal("1.00");
            case STUDENT: return new BigDecimal("0.75");
            default: return BigDecimal.ZERO;
        }
    }

    private BigDecimal getRandomPomeraj() {
        double random = -1.5 + (Math.random() * 3.0); // u rasponu [-1.5, +1.5]
        return BigDecimal.valueOf(random).setScale(2, RoundingMode.HALF_UP);
    }

}