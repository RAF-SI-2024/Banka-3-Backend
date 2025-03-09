package rs.raf.bank_service.specification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.repository.LoanRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
public class LoanScheduler {

    private final LoanRepository loanRepository;
    private final Random random = new Random();

    public LoanScheduler(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?")  // Svakog meseca
    public void updateVariableInterestRates() {
        List<Loan> loans = loanRepository.findByStatus(LoanStatus.APPROVED);

        for (Loan loan : loans) {
            if (loan.getInterestRateType() == InterestRateType.VARIABLE) {
                BigDecimal adjustment = BigDecimal.valueOf(random.nextDouble() * 3 - 1.5); // -1.50% do +1.50%
                loan.setNominalInterestRate(loan.getNominalInterestRate().add(adjustment));
                loanRepository.save(loan);
            }
        }
    }
}