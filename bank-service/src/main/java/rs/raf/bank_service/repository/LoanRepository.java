package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.LoanStatus;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByStatus(LoanStatus loanStatus);
    // Možemo dodati custom metode ako su potrebne
}
