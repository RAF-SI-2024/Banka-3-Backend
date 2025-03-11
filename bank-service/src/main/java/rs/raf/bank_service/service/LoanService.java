package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.EmailRequestDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Installment;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.InstallmentStatus;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.exceptions.LoanRequestNotFoundException;
import rs.raf.bank_service.mappers.LoanMapper;
import rs.raf.bank_service.repository.*;
import rs.raf.bank_service.specification.LoanInterestRateCalculator;
import rs.raf.bank_service.specification.LoanRateCalculator;
import java.util.concurrent.ScheduledExecutorService;
import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanMapper loanMapper;
    private final CurrencyRepository currencyRepository;
    private final InstallmentRepository installmentRepository;
    private final AccountRepository accountRepository;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;
    private final ScheduledExecutorService scheduledExecutorService;

    public List<LoanShortDto> getAllLoans() {
        return loanMapper.toShortDtoList(loanRepository.findAll());
    }

    public Optional<LoanDto> getLoanById(Long id) {
        return loanRepository.findById(id).map(loanMapper::toDto);
    }

    public LoanDto approveLoan(Long id) {
        // prema specifikaciji vadimo podatke iz loanRequest?

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
                .nextInstallmentDate(LocalDate.now())
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

    public LoanDto rejectLoan(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan with ID " + id + " not found"));

        loan.setStatus(LoanStatus.REJECTED);
        loanRepository.save(loan);

        return loanMapper.toDto(loan);
    }
    //svakih 15 sekundi
    @Scheduled(cron = "*/15 * * * * *")
    @Transactional
    public void loanPayment() {
        TransactionStatus status = TransactionAspectSupport.currentTransactionStatus();
        LocalDate today = LocalDate.now();
        List<Loan> loans = loanRepository.findByNextInstallmentDate(today);
        if (loans.isEmpty())
            return;

        for (Loan currLoan : loans) {
            Account currAccount = accountRepository.findByIdForUpdate(currLoan.getAccount().getAccountNumber());
            if (currLoan.getStatus().compareTo(LoanStatus.PAID_OFF) < 0 && currAccount.getBalance().compareTo(currLoan.getNextInstallmentAmount()) >= 0 ) {
                currAccount.setBalance(currAccount.getBalance().subtract(currLoan.getNextInstallmentAmount()));
                accountRepository.save(currAccount);

                List<Installment> installments = currLoan.getInstallments();
                installments.sort(Comparator.comparing(Installment::getId));
                Installment  installment= installments.get(installments.size()-1);
                installment.setInstallmentStatus(InstallmentStatus.PAID);
                installment.setActualDueDate(LocalDate.now());



                if (currLoan.getInstallments().size() < currLoan.getRepaymentPeriod()) {
                    Installment newInstallment = new Installment(currLoan,LoanRateCalculator.calculateMonthlyRate(currLoan.getAmount()
                            ,currLoan.getEffectiveInterestRate(),
                            currLoan.getRepaymentPeriod()) , currLoan.getEffectiveInterestRate(), LocalDate.now(), InstallmentStatus.UNPAID);
                    currLoan.getInstallments().add(newInstallment);
                    currLoan.setNextInstallmentDate(newInstallment.getExpectedDueDate());
                    currLoan.setNextInstallmentAmount(newInstallment.getAmount());
                    loanRepository.save(currLoan);
                } else {
                    currLoan.setStatus(LoanStatus.PAID_OFF);
                }

            } else {
                EmailRequestDto emailRequestDto = new EmailRequestDto();
                emailRequestDto.setCode("INSUFFICIENT-FUNDS");
                Long clientId = currLoan.getAccount().getClientId();
                ClientDto client = userClient.getClientById(clientId);
                emailRequestDto.setDestination(client.getEmail());
                //rabbitTemplate.convertAndSend("insufficient-funds", emailRequestDto);
                //promeniti radi testiranja
                scheduleRetry(currLoan, 72, TimeUnit.HOURS);
                status.setRollbackOnly();
            }
        }
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